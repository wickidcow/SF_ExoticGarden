# ExoticGarden Legacy

**ExoticGarden Legacy** is a compatibility and maintenance fork of the original
ExoticGarden Slimefun addon, focused on preserving the classic addon while
keeping it usable on current Minecraft/Paper servers.

## Foundation 1.0 goals

- Preserve the original `ExoticGarden` plugin identity.
- Preserve existing Slimefun item IDs, recipes, plants, foods, trees and world data.
- Target Paper 26.2 and Java 25.
- Use the stable public Slimefun RC-37 API as the compile baseline while targeting
  Slimefun Legacy at runtime.
- Do not require GuizhanLibPlugin.
- Do not require GuguSlimefunLib.
- Remove the abandoned upstream auto-updater.
- Replace inherited Bukkit/Paper API names that are obsolete on Paper 26.2.
- Produce a deterministic `ExoticGarden-Legacy-1.0.jar` from GitHub Actions.

## Building

Requirements:

- JDK 25
- Maven 3.9+

Before a local build, run:

```bash
python3 scripts/prepare_26_2_sources.py
mvn -B -ntp clean package
```

Output:

```text
target/ExoticGarden-Legacy-1.0.jar
```

GitHub Actions performs the same source-normalization and build steps
automatically.

## Compatibility promise

The Legacy branding is applied to the repository and output JAR, but the Bukkit
plugin name remains `ExoticGarden`. This is deliberate: changing the internal
plugin identity would risk breaking integrations and moving the plugin data
folder.

Foundation 1.0 intentionally avoids balance changes, new crops, renamed IDs,
or recipe redesigns.

## Credits and license

ExoticGarden was originally created by **TheBusyBiscuit** and maintained by the
Slimefun addon community.

This Legacy fork preserves original attribution and remains licensed under the
GNU General Public License v3.0 as required by the upstream project.
