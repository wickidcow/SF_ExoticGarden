# ExoticGarden Legacy

A maintained continuation of the classic **ExoticGarden** Slimefun addon for
modern Paper servers.

The Legacy fork prioritizes preservation: existing items, IDs, recipes, plants,
trees, foods, schematics and world data should continue to behave like classic
ExoticGarden while the implementation is updated for current servers.

## ExoticGarden Legacy 1.1

Version 1.1 is the all-in-one compatibility/stability release.

### Primary target
- Paper 26.2
- Java 25
- Slimefun Legacy

### Compatibility targets
The same JAR is deliberately built against the shared public Slimefun4 API so
it can also run on API-compatible:
- upstream Slimefun4
- Slimefun United
- Gugu Slimefun

No GuizhanLibPlugin or GuguSlimefunLib dependency is required by ExoticGarden
Legacy.

See `COMPATIBILITY.md` for the compatibility contract.

## Building

Requirements:
- JDK 25
- Maven 3.9+

```bash
python3 scripts/prepare_legacy_1_1.py
python3 scripts/verify_legacy_1_1.py
mvn -B -ntp clean package
```

Output:

```text
target/ExoticGarden-Legacy-1.1.jar
```

GitHub Actions performs the same preparation, verification and build sequence.

## Compatibility promise

The release JAR is named `ExoticGarden-Legacy-1.1.jar`, but the Bukkit plugin
name intentionally remains **ExoticGarden**.

That preserves:
- the `plugins/ExoticGarden` data folder
- addon/plugin lookups for `ExoticGarden`
- existing Slimefun registrations
- integrations which expect the classic plugin identity

## Credits

ExoticGarden was created by **TheBusyBiscuit** and maintained by the Slimefun
community. ExoticGarden Legacy preserves that attribution and the upstream
GPLv3 licensing requirements.
