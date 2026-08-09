# ExoticGarden Legacy 1.1 — All-in-One Modernization

Legacy 1.1 combines the originally planned API/storage, generation/stability,
and compatibility/quality phases into one release.

## Slimefun compatibility

- Keeps Slimefun Legacy as the primary runtime target.
- Uses upstream Slimefun RC-37 as the common compile API boundary.
- Avoids direct Slimefun Legacy implementation imports.
- Avoids Slimefun United implementation imports.
- Avoids Gugu-only `StorageCacheUtils` and other fork implementation APIs.
- GuizhanLibPlugin is not required.
- GuguSlimefunLib is not required.
- Adds runtime reporting of the installed Slimefun implementation/version.
- Adds a centralized `BlockStorageCompat` boundary for future storage changes.

## Generation and chunk safety

- Removes the inherited async chunk callbacks from natural generation.
- Keeps world and BlockStorage mutations on the synchronous event path.
- Adds the newer `options.auto-generate-plants` toggle, enabled by default.
- Generates bushes away from chunk borders.
- Reads tree schematic dimensions and keeps tree footprints within the
  currently-populating chunk.
- Skips oversized tree schematics rather than forcing neighboring chunks.
- Uses `World#getHighestBlockYAt` instead of scanning downward from max height.
- Respects modern world minimum/maximum heights while preserving the historic
  Y=30 natural-generation floor.
- Corrects world-border calculations: Bukkit border size is a diameter.
- Respects moved world-border centers instead of assuming the border is
  centered at 0,0.
- Centers flat-ground validation around the target tree.

## Event and storage safety

- Harvest handling moved from MONITOR to HIGHEST because the handler cancels
  and changes the event.
- Adds null-safe right-click handling.
- Uses modern Paper 26.2 block-destroy effects with BlockData.
- Uses modern `SHORT_GRASS` and `ANGRY_VILLAGER` API names.
- Adds piston protection for ExoticGarden-owned Slimefun blocks to prevent a
  vanilla piston from separating a block from its stored Slimefun location.
- Leaves explosion/decay behavior compatible with existing ExoticGarden data.

## Build and regression verification

- Release version: 1.1.
- Exact output: `ExoticGarden-Legacy-1.1.jar`.
- Java 25 / Paper 26.2 build baseline retained.
- Compiler deprecation and unchecked warnings enabled.
- Whole-source normalization for removed Paper API names.
- CI rejects Gugu/Guizhan implementation imports.
- CI rejects hard Guizhan/Gugu dependencies.
- CI checks that the internal Bukkit plugin name remains `ExoticGarden`.
- CI checks that the listener cannot regress to `getChunkAtAsync`/`thenRun`
  world mutation.
- CI checks that the finished JAR did not shade Gugu/Guizhan implementation
  classes.
- CI emits an explicit Slimefun-ID manifest for diagnostics.

## Preserved behavior/data

- No intentional Slimefun ID renames.
- No recipe redesign.
- No crop/food rebalance.
- No schematic renames.
- No plugin data-folder rename.
- No automatic updater back to the abandoned upstream project.
