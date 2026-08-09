# ExoticGarden Legacy 1.0 — Foundation / Paper 26.2

This is the first modernization layer for ExoticGarden Legacy.

## Included

### Build platform
- Maven project version set to `1.0`.
- Java release set to 25.
- Paper API pinned to `26.2.build.111-stable`.
- Slimefun API baseline moved from RC-27 to RC-37.
- Old Destroystokyo and Spigot repositories removed.
- Current Paper Maven repository added.
- Maven Compiler Plugin updated to 3.15.0.
- Maven Shade Plugin updated to 3.6.2.
- bStats updated and shaded under the existing ExoticGarden namespace.
- Exact final JAR name: `ExoticGarden-Legacy-1.0.jar`.

### Dependency policy
- `Slimefun` remains the only hard plugin dependency.
- No GuizhanLibPlugin dependency.
- No GuguSlimefunLib dependency.
- Paper API and Slimefun remain provided/compile-only server dependencies.

### Runtime/source compatibility
The source normalizer updates inherited APIs that are incompatible or being
removed on Paper 26.2:

- `Material.GRASS` -> `Material.SHORT_GRASS`
- `Particle.VILLAGER_ANGRY` -> `Particle.ANGRY_VILLAGER`
- `Effect.STEP_SOUND` -> `Effect.DESTROY_BLOCK`
- The new block-break effect receives `BlockData` via
  `Material.OAK_LEAVES.createBlockData()`.
- The abandoned original ExoticGarden auto-updater is removed.
- The bug tracker is redirected to the Legacy fork.

### Compatibility safeguards
Foundation 1.0 deliberately keeps:
- Bukkit plugin name `ExoticGarden`
- main class/package names
- Slimefun item IDs
- item group keys
- recipes
- plant/tree/berry identifiers
- schematic filenames
- existing data-folder identity

## CI
`.github/workflows/maven.yml`:

- Builds on Ubuntu.
- Uses Java 25.
- Runs the Paper 26.2 source normalizer.
- Runs Maven clean/package.
- Fails if the exact release JAR is missing or empty.
- Prints the JAR SHA-256 and embedded plugin.yml.
- Uploads `ExoticGarden-Legacy-1.0.jar` as a GitHub Actions artifact.

## Next Foundation work after first green build
The inherited code should then be audited in focused passes for:

1. Slimefun `BlockStorage` compatibility and deprecation cleanup.
2. Paper 26.2 event/API warnings that do not currently block compilation.
3. Chunk generation and async PaperLib safety.
4. Schematic placement boundaries and modern world-height handling.
5. Selective backports of useful post-2022 ExoticGarden fixes without
   importing Gugu/Guizhan dependencies.
6. Add-on interoperability checks with Gastronomicon and other consumers of
   ExoticGarden item IDs.
