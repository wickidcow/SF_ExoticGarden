package io.github.thebusybiscuit.exoticgarden.compat;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Compatibility boundary around Slimefun's current block-data controller.
 *
 * <p>All synchronous reads explicitly load block data before accessing it so
 * legacy ExoticGarden harvest/tree behavior remains unchanged.</p>
 */
public final class BlockStorageCompat {

    private BlockStorageCompat() {
    }

    public static SlimefunItem check(Block block) {
        return block == null ? null : check(block.getLocation());
    }

    public static SlimefunItem check(Location location) {
        if (location == null) {
            return null;
        }

        var controller = Slimefun.getDatabaseManager().getBlockDataController();
        var data = controller.getBlockData(location);
        if (data == null) {
            return null;
        }

        if (!data.isDataLoaded()) {
            controller.loadBlockData(data);
        }

        return SlimefunItem.getById(data.getSfId());
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
        if (slimefunItem != null) {
            Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .createBlock(block.getLocation(), slimefunItem.getId());
        }
    }

    public static void clear(Location location) {
        if (location != null) {
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
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
}
