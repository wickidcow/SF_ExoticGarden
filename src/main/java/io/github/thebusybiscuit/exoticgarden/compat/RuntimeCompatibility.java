package io.github.thebusybiscuit.exoticgarden.compat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Runtime diagnostics which intentionally depend only on Bukkit's Plugin API.
 *
 * <p>ExoticGarden Legacy must not hard-link against Slimefun Legacy, United,
 * Gugu, GuizhanLib or any other fork-specific implementation class just to
 * identify the runtime. The shared Slimefun API remains the compatibility
 * contract, while this class only reports what is actually installed.</p>
 */
public final class RuntimeCompatibility {

    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();
    private static volatile boolean logged;

    private RuntimeCompatibility() {
    }

    public static void logStartup(JavaPlugin addon, boolean enabled) {
        if (!enabled || logged) {
            return;
        }

        logged = true;

        Plugin slimefun = Bukkit.getPluginManager().getPlugin("Slimefun");
        if (slimefun == null) {
            addon.getLogger().warning(
                "[Compatibility] Slimefun was not visible during ExoticGarden startup."
            );
            return;
        }

        PluginDescriptionFile description = slimefun.getDescription();

        addon.getLogger().info(
            "[Compatibility] Slimefun runtime: "
                + description.getName()
                + " "
                + description.getVersion()
                + " | main="
                + slimefun.getClass().getName()
        );

        addon.getLogger().info(
            "[Compatibility] Shared Slimefun API mode enabled "
                + "(Legacy primary; Slimefun4/United/Gugu-compatible where their public API is preserved)."
        );

        logOptionalLibrary(addon, "GuizhanLibPlugin");
        logOptionalLibrary(addon, "GuguSlimefunLib");
    }

    private static void logOptionalLibrary(JavaPlugin addon, String pluginName) {
        Plugin optional = Bukkit.getPluginManager().getPlugin(pluginName);

        addon.getLogger().info(
            "[Compatibility] Optional "
                + pluginName
                + ": "
                + (optional != null && optional.isEnabled() ? "present" : "not present")
                + " (not required)"
        );
    }

    public static void warnOnce(
        JavaPlugin addon,
        String key,
        String message,
        Throwable throwable
    ) {
        if (!WARNED_KEYS.add(key)) {
            return;
        }

        if (throwable == null) {
            addon.getLogger().warning(message);
        }
        else {
            addon.getLogger().log(Level.WARNING, message, throwable);
        }
    }
}
