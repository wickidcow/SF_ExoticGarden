package io.github.thebusybiscuit.exoticgarden.compat;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Cross-runtime storage boundary.
 *
 * <p>Slimefun Legacy exposes the modern block-data controller, while the
 * RC-37/United compatibility baselines still expose only the legacy
 * BlockStorage facade. This adapter prefers the modern controller whenever it
 * exists and falls back only when running on a runtime that does not provide
 * it. No deprecated storage type is linked into ExoticGarden bytecode.</p>
 */
public final class BlockStorageCompat {

    private static final String LEGACY_BLOCK_STORAGE =
            "me.mrCookieSlime.Slimefun.api.BlockStorage";

    private BlockStorageCompat() {
    }

    public static SlimefunItem check(Block block) {
        return block == null ? null : check(block.getLocation());
    }

    public static SlimefunItem check(Location location) {
        if (location == null) {
            return null;
        }

        Object controller = modernController();
        if (controller != null) {
            Object data = invoke(controller, "getBlockData", new Class<?>[] { Location.class }, location);
            if (data == null) {
                return null;
            }

            Object id = invoke(data, "getSfId", new Class<?>[0]);
            return id instanceof String sfId ? SlimefunItem.getById(sfId) : null;
        }

        Object item = invokeLegacy("check", new Class<?>[] { Location.class }, location);
        return item instanceof SlimefunItem slimefunItem ? slimefunItem : null;
    }

    public static String checkId(Block block) {
        SlimefunItem item = check(block);
        return item == null ? null : item.getId();
    }

    public static ItemStack retrieve(Block block) {
        if (block == null) {
            return null;
        }

        SlimefunItem item = check(block);
        if (item == null) {
            return null;
        }

        clear(block.getLocation());
        return item.getItem();
    }

    public static void store(Block block, ItemStack item) {
        if (block == null || item == null) {
            return;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem == null) {
            return;
        }

        Object controller = modernController();
        if (controller != null) {
            invoke(
                    controller,
                    "createBlock",
                    new Class<?>[] { Location.class, String.class },
                    block.getLocation(),
                    slimefunItem.getId()
            );
            return;
        }

        invokeLegacy("store", new Class<?>[] { Block.class, ItemStack.class }, block, item);
    }

    public static void clear(Location location) {
        if (location == null) {
            return;
        }

        Object controller = modernController();
        if (controller != null) {
            invoke(controller, "removeBlock", new Class<?>[] { Location.class }, location);
            return;
        }

        invokeLegacy("clearBlockInfo", new Class<?>[] { Location.class }, location);
    }

    public static void remove(Location location) {
        clear(location);
    }

    public static void replace(Block block, ItemStack item) {
        if (block == null || item == null) {
            return;
        }

        clear(block.getLocation());
        store(block, item);
    }

    public static boolean isOwnedBy(Block block, SlimefunAddon addon) {
        SlimefunItem item = check(block);

        if (item == null || item.getAddon() == null || addon == null) {
            return false;
        }

        return item.getAddon().getJavaPlugin() == addon.getJavaPlugin();
    }

    private static Object modernController() {
        try {
            Method databaseManagerMethod = Slimefun.class.getMethod("getDatabaseManager");
            Object databaseManager = databaseManagerMethod.invoke(null);
            return databaseManager.getClass().getMethod("getBlockDataController").invoke(databaseManager);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Could not access Slimefun's modern block-data controller", ex);
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Could not invoke modern Slimefun storage method " + methodName, ex);
        }
    }

    private static Object invokeLegacy(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> blockStorage = Class.forName(LEGACY_BLOCK_STORAGE, true, Slimefun.class.getClassLoader());
            return blockStorage.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(
                    "Neither modern Slimefun storage nor the compatibility storage facade is available",
                    ex
            );
        }
    }
}
