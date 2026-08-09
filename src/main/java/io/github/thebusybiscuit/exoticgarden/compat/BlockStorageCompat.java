package io.github.thebusybiscuit.exoticgarden.compat;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Small compatibility boundary around the legacy BlockStorage API.
 *
 * <p>RC-37, Slimefun Legacy and the major continuation forks retain this API
 * surface for addon/save compatibility. Keeping direct storage calls in one
 * class makes future storage migrations local instead of scattering
 * fork-specific calls through ExoticGarden gameplay code.</p>
 */
public final class BlockStorageCompat {

    private BlockStorageCompat() {
    }

    public static SlimefunItem check(Block block) {
        return block == null ? null : BlockStorage.check(block);
    }

    public static SlimefunItem check(Location location) {
        return location == null ? null : BlockStorage.check(location);
    }

    public static String checkId(Block block) {
        SlimefunItem item = check(block);
        return item == null ? null : item.getId();
    }

    public static ItemStack retrieve(Block block) {
        return block == null ? null : BlockStorage.retrieve(block);
    }

    public static void store(Block block, ItemStack item) {
        if (block != null && item != null) {
            BlockStorage.store(block, item);
        }
    }

    public static void clear(Location location) {
        if (location != null) {
            BlockStorage.clearBlockInfo(location);
        }
    }

    public static void replace(Block block, ItemStack item) {
        if (block == null || item == null) {
            return;
        }

        Location location = block.getLocation();
        BlockStorage.deleteLocationInfoUnsafely(location, false);
        BlockStorage.store(block, item);
    }

    /**
     * Returns true only when the Slimefun block was registered by this addon.
     */
    public static boolean isOwnedBy(Block block, SlimefunAddon addon) {
        SlimefunItem item = check(block);

        if (item == null || item.getAddon() == null || addon == null) {
            return false;
        }

        return item.getAddon().getJavaPlugin() == addon.getJavaPlugin();
    }
}
