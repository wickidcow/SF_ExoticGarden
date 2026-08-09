# ExoticGarden Legacy 1.1 Compatibility Contract

## Runtime targets

ExoticGarden Legacy uses a **shared-API strategy** rather than building a
different JAR for every Slimefun fork.

Priority:

1. **Slimefun Legacy** — primary runtime target.
2. **Slimefun4** — shared public API baseline.
3. **Slimefun United** — compatibility target where its inherited Slimefun4 API
   remains available.
4. **Gugu Slimefun** — compatibility target where its inherited Slimefun4 API
   remains available.

## What "one JAR" means

The Maven project compiles against upstream Slimefun **RC-37** and does not
compile against:

- Slimefun Legacy implementation classes
- Slimefun United implementation classes
- Gugu StorageCacheUtils or other Gugu-only storage classes
- GuizhanLibPlugin
- GuguSlimefunLib

At runtime, ExoticGarden Legacy reports the installed `Slimefun` plugin version
and implementation class using only Bukkit's Plugin API.

This avoids class-loader failures caused merely by probing a fork-specific API.

## Storage policy

The addon currently retains Slimefun's compatibility `BlockStorage` API behind
`BlockStorageCompat`.

That is deliberate. It is the lowest common storage surface inherited across
the Slimefun4 family and keeps old ExoticGarden world data readable.

If a future fork removes that API entirely, only the compatibility boundary
should need a storage bridge rather than every plant listener.

## Data compatibility

Legacy 1.1 intentionally preserves:

- Bukkit plugin name: `ExoticGarden`
- main class/package identity
- existing Slimefun IDs
- recipe IDs and item-group keys
- berry/tree/plant naming rules
- schematic file names
- `plugins/ExoticGarden` data-folder identity

No automatic migration is required for a normal upgrade from classic
ExoticGarden/Legacy 1.0.

## Optional libraries

`GuizhanLibPlugin` and `GuguSlimefunLib` may be present on a server for other
addons, but ExoticGarden Legacy does not declare or use them as hard
dependencies.
