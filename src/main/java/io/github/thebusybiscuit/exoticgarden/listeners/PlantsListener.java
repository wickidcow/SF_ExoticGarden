package io.github.thebusybiscuit.exoticgarden.listeners;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import io.github.thebusybiscuit.exoticgarden.Berry;
import io.github.thebusybiscuit.exoticgarden.ExoticGarden;
import io.github.thebusybiscuit.exoticgarden.PlantType;
import io.github.thebusybiscuit.exoticgarden.Tree;
import io.github.thebusybiscuit.exoticgarden.compat.BlockStorageCompat;
import io.github.thebusybiscuit.exoticgarden.compat.RuntimeCompatibility;
import io.github.thebusybiscuit.exoticgarden.items.BonemealableItem;
import io.github.thebusybiscuit.exoticgarden.schematics.Schematic;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;

/**
 * ExoticGarden's plant/tree listener.
 *
 * <p>Legacy 1.1 deliberately keeps this listener on the shared Slimefun4 API
 * surface. It does not import Gugu StorageCacheUtils, United-only classes or
 * Slimefun Legacy internals.</p>
 *
 * <p>The old listener attempted asynchronous chunk acquisition and then
 * mutated blocks/storage from completion callbacks. ChunkPopulateEvent and
 * StructureGrowEvent already give us the loaded world context we need, so
 * 1.1 keeps all world/BlockStorage mutation on the event thread.</p>
 */
public class PlantsListener implements Listener {

    private static final int DEFAULT_MINIMUM_GENERATION_Y = 30;
    private static final int BUSH_CHUNK_MARGIN = 3;

    private final Config cfg;
    private final ExoticGarden plugin;
    private final BlockFace[] faces = {
        BlockFace.NORTH,
        BlockFace.NORTH_EAST,
        BlockFace.EAST,
        BlockFace.SOUTH_EAST,
        BlockFace.SOUTH,
        BlockFace.SOUTH_WEST,
        BlockFace.WEST,
        BlockFace.NORTH_WEST
    };

    public PlantsListener(ExoticGarden plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getCfg();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        RuntimeCompatibility.logStartup(
            plugin,
            cfg.getOrSetDefault("compatibility.log-runtime", true)
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent event) {
        // StructureGrowEvent is synchronous. Do not defer its cancellation or
        // block/storage mutations to an async completion callback.
        growStructure(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGenerate(ChunkPopulateEvent event) {
        if (!cfg.getOrSetDefault("options.auto-generate-plants", true)) {
            return;
        }

        final World world = event.getWorld();

        if (!Slimefun.getWorldSettingsService().isWorldEnabled(world)) {
            return;
        }

        if (cfg.getStringList("world-blacklist").contains(world.getName())) {
            return;
        }

        try {
            generateInPopulatingChunk(event);
        }
        catch (RuntimeException ex) {
            RuntimeCompatibility.warnOnce(
                plugin,
                "generation-" + world.getName(),
                "[Generation] ExoticGarden skipped a failed natural-generation attempt in "
                    + world.getName()
                    + ". Further identical warnings for this world are suppressed.",
                ex
            );
        }
    }

    private void generateInPopulatingChunk(ChunkPopulateEvent event) {
        Random random = ThreadLocalRandom.current();

        if (
            !ExoticGarden.getBerries().isEmpty()
                && random.nextInt(100) < clampChance(cfg.getInt("chances.BUSH"))
        ) {
            Berry berry = ExoticGarden.getBerries().get(
                random.nextInt(ExoticGarden.getBerries().size())
            );

            // Ore plants are progression items and have never been part of
            // normal world generation.
            if (berry.getType() == PlantType.ORE_PLANT) {
                return;
            }

            int chunkX = event.getChunk().getX();
            int chunkZ = event.getChunk().getZ();

            // Stay away from chunk edges so block updates do not load or touch
            // neighboring chunks while this chunk is being populated.
            int x = chunkX * 16
                + BUSH_CHUNK_MARGIN
                + random.nextInt(16 - (BUSH_CHUNK_MARGIN * 2));
            int z = chunkZ * 16
                + BUSH_CHUNK_MARGIN
                + random.nextInt(16 - (BUSH_CHUNK_MARGIN * 2));

            if (isInsideWorldBorder(event.getWorld(), x, z, BUSH_CHUNK_MARGIN)) {
                growBush(event, x, z, berry, random);
            }

            return;
        }

        if (
            !ExoticGarden.getTrees().isEmpty()
                && random.nextInt(100) < clampChance(cfg.getInt("chances.TREE"))
        ) {
            Tree tree = ExoticGarden.getTrees().get(
                random.nextInt(ExoticGarden.getTrees().size())
            );

            generateTree(event, tree, random);
        }
    }

    private void generateTree(ChunkPopulateEvent event, Tree tree, Random random) {
        int schematicWidth = 5;
        int schematicLength = 5;

        try {
            schematicWidth = Math.max(1, tree.getSchematic().getWidth());
            schematicLength = Math.max(1, tree.getSchematic().getLength());
        }
        catch (IOException ex) {
            RuntimeCompatibility.warnOnce(
                plugin,
                "schematic-size-" + tree.getFruitID(),
                "[Generation] Could not read dimensions for tree "
                    + tree.getFruitID()
                    + "; using the classic safe fallback footprint.",
                ex
            );
        }

        // Two blocks of padding prevents the schematic and neighbor updates
        // from crossing into a chunk which is not being populated.
        int paddedWidth = schematicWidth + 2;
        int paddedLength = schematicLength + 2;

        if (paddedWidth >= 16 || paddedLength >= 16) {
            RuntimeCompatibility.warnOnce(
                plugin,
                "schematic-too-wide-" + tree.getFruitID(),
                "[Generation] Skipping natural generation of "
                    + tree.getFruitID()
                    + " because its schematic cannot safely fit inside one chunk.",
                null
            );
            return;
        }

        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();

        int xRange = 16 - paddedWidth;
        int zRange = 16 - paddedLength;

        if (xRange <= 0 || zRange <= 0) {
            return;
        }

        int x = chunkX * 16
            + random.nextInt(xRange)
            + (paddedWidth / 2);
        int z = chunkZ * 16
            + random.nextInt(zRange)
            + (paddedLength / 2);

        int borderMargin = Math.max(paddedWidth, paddedLength) / 2;

        if (isInsideWorldBorder(event.getWorld(), x, z, borderMargin)) {
            pasteTree(event, x, z, tree);
        }
    }

    private int clampChance(int chance) {
        return Math.max(0, Math.min(100, chance));
    }

    /**
     * WorldBorder#getSize() is the border DIAMETER, not an absolute coordinate
     * limit. The old ExoticGarden code treated it as +/- size around 0,0 and
     * ignored moved world-border centers.
     */
    private boolean isInsideWorldBorder(
        World world,
        int x,
        int z,
        int margin
    ) {
        double halfSize = world.getWorldBorder().getSize() / 2.0D;
        Location center = world.getWorldBorder().getCenter();

        double minX = center.getX() - halfSize + margin;
        double maxX = center.getX() + halfSize - margin;
        double minZ = center.getZ() - halfSize + margin;
        double maxZ = center.getZ() + halfSize - margin;

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private int getMinimumGenerationY(World world) {
        // Preserve ExoticGarden's historic Y=30 floor while also respecting
        // modern negative world heights.
        return Math.max(world.getMinHeight() + 1, DEFAULT_MINIMUM_GENERATION_Y);
    }

    private void growStructure(StructureGrowEvent event) {
        SlimefunItem item = BlockStorageCompat.check(event.getLocation().getBlock());

        if (item == null) {
            return;
        }

        for (Tree tree : ExoticGarden.getTrees()) {
            if (item.getId().equalsIgnoreCase(tree.getSapling())) {
                event.setCancelled(true);
                BlockStorageCompat.clear(event.getLocation());
                Schematic.pasteSchematic(event.getLocation(), tree);
                return;
            }
        }

        for (Berry berry : ExoticGarden.getBerries()) {
            if (!item.getId().equalsIgnoreCase(berry.toBush())) {
                continue;
            }

            event.setCancelled(true);

            switch (berry.getType()) {
                case BUSH:
                    event.getLocation().getBlock().setType(Material.OAK_LEAVES, false);
                    break;

                case ORE_PLANT:
                case DOUBLE_PLANT:
                    Block blockAbove = event
                        .getLocation()
                        .getBlock()
                        .getRelative(BlockFace.UP);

                    if (BlockStorageCompat.check(blockAbove) != null) {
                        return;
                    }

                    if (
                        !Tag.SAPLINGS.isTagged(blockAbove.getType())
                            && !Tag.LEAVES.isTagged(blockAbove.getType())
                    ) {
                        switch (blockAbove.getType()) {
                            case AIR:
                            case CAVE_AIR:
                            case SNOW:
                                break;
                            default:
                                return;
                        }
                    }

                    BlockStorageCompat.store(blockAbove, berry.getItem());
                    event.getLocation().getBlock().setType(Material.OAK_LEAVES, false);
                    blockAbove.setType(Material.PLAYER_HEAD, false);

                    Rotatable upperRotation = (Rotatable) blockAbove.getBlockData();
                    upperRotation.setRotation(
                        faces[ThreadLocalRandom.current().nextInt(faces.length)]
                    );
                    blockAbove.setBlockData(upperRotation, false);
                    PlayerHead.setSkin(
                        blockAbove,
                        PlayerSkin.fromHashCode(berry.getTexture()),
                        true
                    );
                    break;

                default:
                    event.getLocation().getBlock().setType(Material.PLAYER_HEAD, false);

                    Rotatable rotation = (Rotatable) event
                        .getLocation()
                        .getBlock()
                        .getBlockData();

                    rotation.setRotation(
                        faces[ThreadLocalRandom.current().nextInt(faces.length)]
                    );

                    event.getLocation().getBlock().setBlockData(rotation, false);
                    PlayerHead.setSkin(
                        event.getLocation().getBlock(),
                        PlayerSkin.fromHashCode(berry.getTexture()),
                        true
                    );
                    break;
            }

            BlockStorageCompat.replace(event.getLocation().getBlock(), berry.getItem());

            event.getWorld().playEffect(
                event.getLocation(),
                Effect.DESTROY_BLOCK,
                Material.OAK_LEAVES.createBlockData()
            );

            return;
        }
    }

    private void pasteTree(
        ChunkPopulateEvent event,
        int x,
        int z,
        Tree tree
    ) {
        World world = event.getWorld();
        int startY = Math.min(
            world.getMaxHeight() - 1,
            world.getHighestBlockYAt(x, z) + 2
        );

        int minimumY = getMinimumGenerationY(world);

        for (int y = startY; y >= minimumY; y--) {
            Block current = world.getBlockAt(x, y, z);

            if (
                !current.getType().isSolid()
                    && current.getType() != Material.WATER
                    && current.getType() != Material.SEAGRASS
                    && current.getType() != Material.TALL_SEAGRASS
                    && !(
                        current.getBlockData() instanceof Waterlogged waterlogged
                            && waterlogged.isWaterlogged()
                    )
                    && tree.isSoil(current.getRelative(BlockFace.DOWN).getType())
                    && isFlat(current)
            ) {
                Schematic.pasteSchematic(new Location(world, x, y, z), tree);
                return;
            }
        }
    }

    private void growBush(
        ChunkPopulateEvent event,
        int x,
        int z,
        Berry berry,
        Random random
    ) {
        World world = event.getWorld();
        int startY = Math.min(
            world.getMaxHeight() - 1,
            world.getHighestBlockYAt(x, z) + 2
        );

        int minimumY = getMinimumGenerationY(world);

        for (int y = startY; y >= minimumY; y--) {
            Block current = world.getBlockAt(x, y, z);

            if (
                !current.getType().isSolid()
                    && current.getType() != Material.WATER
                    && berry.isSoil(current.getRelative(BlockFace.DOWN).getType())
            ) {
                BlockStorageCompat.store(current, berry.getItem());

                switch (berry.getType()) {
                    case BUSH:
                        current.setType(Material.OAK_LEAVES, false);
                        break;

                    case FRUIT:
                    case DOUBLE_PLANT:
                        current.setType(Material.PLAYER_HEAD, false);

                        Rotatable rotation = (Rotatable) current.getBlockData();
                        rotation.setRotation(faces[random.nextInt(faces.length)]);
                        current.setBlockData(rotation, false);

                        PlayerHead.setSkin(
                            current,
                            PlayerSkin.fromHashCode(berry.getTexture()),
                            true
                        );
                        break;

                    case ORE_PLANT:
                        // ORE_PLANT is filtered before this method is called.
                        return;

                    default:
                        return;
                }

                return;
            }
        }
    }

    private boolean isFlat(Block current) {
        // Center the old 5x5 footprint around the target instead of scanning
        // only positive X/Z directions.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!current.getRelative(x, -1, z).getType().isSolid()) {
                    return false;
                }

                for (int y = 0; y < 6; y++) {
                    Block block = current.getRelative(x, y, z);

                    if (
                        block.getType().isSolid()
                            || Tag.LEAVES.isTagged(block.getType())
                    ) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (
            !Slimefun.getProtectionManager().hasPermission(
                event.getPlayer(),
                event.getBlock().getLocation(),
                Interaction.BREAK_BLOCK
            )
        ) {
            return;
        }

        Block block = event.getBlock();

        if (
            block.getType() == Material.PLAYER_HEAD
                || Tag.LEAVES.isTagged(block.getType())
        ) {
            dropFruitFromTree(block);
        }

        if (block.getType() == Material.SHORT_GRASS) {
            if (
                !ExoticGarden.getGrassDrops().isEmpty()
                    && event.getPlayer().getGameMode() != GameMode.CREATIVE
            ) {
                Random random = ThreadLocalRandom.current();

                if (random.nextInt(100) < 6) {
                    ItemStack[] items = ExoticGarden
                        .getGrassDrops()
                        .values()
                        .toArray(new ItemStack[0]);

                    block.getWorld().dropItemNaturally(
                        block.getLocation(),
                        items[random.nextInt(items.length)]
                    );
                }
            }

            return;
        }

        ItemStack item = ExoticGarden.harvestPlant(block);

        if (item != null) {
            event.setCancelled(true);
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();

        if (!Slimefun.getWorldSettingsService().isWorldEnabled(block.getWorld())) {
            return;
        }

        String id = BlockStorageCompat.checkId(block);

        if (id != null) {
            for (Berry berry : ExoticGarden.getBerries()) {
                if (id.equalsIgnoreCase(berry.getID())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        dropFruitFromTree(block);

        ItemStack item = BlockStorageCompat.retrieve(block);

        if (item != null) {
            event.setCancelled(true);
            BlockStorageCompat.clear(block.getLocation());
            block.setType(Material.AIR, false);
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getPlayer().isSneaking() || event.getClickedBlock() == null) {
            return;
        }

        Block clicked = event.getClickedBlock();

        if (
            !Slimefun.getProtectionManager().hasPermission(
                event.getPlayer(),
                clicked.getLocation(),
                Interaction.BREAK_BLOCK
            )
        ) {
            return;
        }

        ItemStack item = ExoticGarden.harvestPlant(clicked);

        if (item != null) {
            clicked.getWorld().playEffect(
                clicked.getLocation(),
                Effect.DESTROY_BLOCK,
                Material.OAK_LEAVES.createBlockData()
            );
            clicked.getWorld().dropItemNaturally(clicked.getLocation(), item);
        }
        else {
            ExoticGarden.getInstance().harvestFruit(clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeAll(getAffectedBlocks(event.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeAll(getAffectedBlocks(event.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!cfg.getOrSetDefault("compatibility.protect-piston-movement", true)) {
            return;
        }

        for (Block block : event.getBlocks()) {
            if (BlockStorageCompat.isOwnedBy(block, plugin)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!cfg.getOrSetDefault("compatibility.protect-piston-movement", true)) {
            return;
        }

        for (Block block : event.getBlocks()) {
            if (BlockStorageCompat.isOwnedBy(block, plugin)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBonemealPlant(BlockFertilizeEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.OAK_SAPLING) {
            return;
        }

        SlimefunItem item = BlockStorageCompat.check(block);

        if (
            item instanceof BonemealableItem bonemealable
                && bonemealable.isBonemealDisabled()
        ) {
            event.setCancelled(true);

            block.getWorld().spawnParticle(
                Particle.ANGRY_VILLAGER,
                block.getLocation().clone().add(0.5, 0, 0.5),
                4
            );

            block.getWorld().playSound(
                block.getLocation(),
                Sound.ENTITY_VILLAGER_NO,
                1,
                1
            );
        }
    }

    private Set<Block> getAffectedBlocks(List<Block> blockList) {
        Set<Block> blocksToRemove = new HashSet<>();

        for (Block block : blockList) {
            ItemStack item = ExoticGarden.harvestPlant(block);

            if (item != null) {
                blocksToRemove.add(block);
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        return blocksToRemove;
    }

    private void dropFruitFromTree(Block block) {
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    Block fruit = block.getRelative(x, y, z);

                    if (fruit.isEmpty()) {
                        continue;
                    }

                    Location location = fruit.getLocation();
                    SlimefunItem item = BlockStorageCompat.check(location);

                    if (item == null) {
                        continue;
                    }

                    for (Tree tree : ExoticGarden.getTrees()) {
                        if (item.getId().equalsIgnoreCase(tree.getFruitID())) {
                            BlockStorageCompat.clear(location);

                            fruit.getWorld().playEffect(
                                location,
                                Effect.DESTROY_BLOCK,
                                Material.OAK_LEAVES.createBlockData()
                            );

                            fruit.getWorld().dropItemNaturally(
                                location,
                                item.getItem()
                            );

                            fruit.setType(Material.AIR, false);
                            break;
                        }
                    }
                }
            }
        }
    }
}
