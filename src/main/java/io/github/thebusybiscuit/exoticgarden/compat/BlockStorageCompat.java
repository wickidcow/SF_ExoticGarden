package io.github.thebusybiscuit.exoticgarden.compat;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Cross-runtime Slimefun block-storage compatibility boundary.
 *
 * <p>Slimefun Legacy exposes the modern database/block-data controller while
 * RC-37/United-compatible runtimes retain the historical BlockStorage facade.
 * This adapter selects the available runtime API without hard-linking addon
 * source to either implementation-specific storage type.</p>
 */
public final class BlockStorageCompat {

    private static final String SLIMEFUN_CLASS =
            "io.github.thebusybiscuit.slimefun4.implementation.Slimefun";
    private static final String LEGACY_STORAGE_CLASS =
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

        String id = modernCheckId(location);
        if (id == null) {
            id = legacyCheckId(location);
        }

        return id == null ? null : SlimefunItem.getById(id);
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

        if (!modernCreate(block.getLocation(), slimefunItem.getId())) {
            legacyStore(block, item);
        }
    }

    public static void clear(Location location) {
        if (location == null) {
            return;
        }

        if (!modernRemove(location)) {
            legacyClear(location);
        }
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

    private static String modernCheckId(Location location) {
        try {
            Object controller = modernController();
            if (controller == null) {
                return null;
            }

            Method getBlockData = controller.getClass().getMethod("getBlockData", Location.class);
            Object data = getBlockData.invoke(controller, location);
            if (data == null) {
                return null;
            }

            Method getSfId = data.getClass().getMethod("getSfId");
            Object id = getSfId.invoke(data);
            return id instanceof String string ? string : null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean modernCreate(Location location, String id) {
        try {
            Object controller = modernController();
            if (controller == null) {
                return false;
            }

            Method createBlock = controller.getClass().getMethod("createBlock", Location.class, String.class);
            createBlock.invoke(controller, location, id);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean modernRemove(Location location) {
        try {
            Object controller = modernController();
            if (controller == null) {
                return false;
            }

            Method removeBlock = controller.getClass().getMethod("removeBlock", Location.class);
            removeBlock.invoke(controller, location);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            return false;
        }
    }

    private static Object modernController()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> slimefunClass = Class.forName(SLIMEFUN_CLASS, false, BlockStorageCompat.class.getClassLoader());
        Method getDatabaseManager = slimefunClass.getMethod("getDatabaseManager");
        Object databaseManager = getDatabaseManager.invoke(null);
        if (databaseManager == null) {
            return null;
        }

        Method getBlockDataController = databaseManager.getClass().getMethod("getBlockDataController");
        return getBlockDataController.invoke(databaseManager);
    }

    private static String legacyCheckId(Location location) {
        try {
            Class<?> storage = Class.forName(LEGACY_STORAGE_CLASS, false, BlockStorageCompat.class.getClassLoader());

            try {
                Method checkId = storage.getMethod("checkID", Location.class);
                Object id = checkId.invoke(null, location);
                return id instanceof String string ? string : null;
            } catch (NoSuchMethodException ignored) {
                Method getLocationInfo = storage.getMethod("getLocationInfo", Location.class, String.class);
                Object id = getLocationInfo.invoke(null, location, "id");
                return id instanceof String string ? string : null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static void legacyStore(Block block, ItemStack item) {
        try {
            Class<?> storage = Class.forName(LEGACY_STORAGE_CLASS, false, BlockStorageCompat.class.getClassLoader());
            Method store = storage.getMethod("store", Block.class, ItemStack.class);
            store.invoke(null, block, item);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            // No compatible storage backend is available.
        }
    }

    private static void legacyClear(Location location) {
        try {
            Class<?> storage = Class.forName(LEGACY_STORAGE_CLASS, false, BlockStorageCompat.class.getClassLoader());
            Method clear = storage.getMethod("clearBlockInfo", Location.class);
            clear.invoke(null, location);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException | LinkageError ignored) {
            // No compatible storage backend is available.
        }
    }
}
