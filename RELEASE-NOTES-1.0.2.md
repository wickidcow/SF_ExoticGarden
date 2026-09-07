# SF_ExoticGarden 1.0.2

## Universal Slimefun compatibility build

This release removes the hidden build prerequisite that caused ExoticGarden to appear broken in Slimefun Legacy's direct addon compatibility matrix.

### What changed

- Runs the existing cross-version source normalizer automatically from the normal Maven `generate-sources` lifecycle.
- A plain `mvn package` now normalizes removed Bukkit/Paper symbols before compilation instead of requiring a separate preparation command.
- Keeps the addon compiled against the shared Slimefun4 API surface rather than Slimefun Legacy-only, Gugu-only, United-only, or Guizhan-only implementation classes.
- Keeps `Slimefun` as the only hard plugin dependency; GuizhanLibPlugin and GuguSlimefunLib remain optional.
- Verifies the same source from a clean inherited state against Paper 1.21.11 and Paper 26.2.
- Emits Java 21 bytecode for the release JAR.
- Release artifact: `SF_ExoticGarden1.0.2.jar`.

### Slimefun runtime target

The JAR is intended to run on Slimefun Legacy and other Slimefun4-family runtimes that preserve the shared public API used by ExoticGarden, including Original-compatible, United-compatible, and Gugu-compatible builds. It intentionally does not hard-link to fork-specific implementation APIs.

### Compatibility note

No single addon can guarantee every historical or future Slimefun build. This release maximizes portability by using the shared Slimefun4 API contract and runtime-safe Bukkit/Paper compatibility helpers instead of a Legacy-only dependency.
