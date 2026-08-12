<div align="center">

# 🌴🍓 ExoticGarden — Slimefun Legacy

**The classic Slimefun garden: plants, fruit trees, foods, and recipes preserved for modern servers.**

![Slimefun Legacy](https://img.shields.io/badge/Slimefun-Legacy-6bd425?style=for-the-badge)
![Paper 26.2](https://img.shields.io/badge/Paper-26.2-blue?style=for-the-badge)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)
![Maintained for AlbionMC.com](https://img.shields.io/badge/Maintained%20for-albionmc.com-7b68ee?style=for-the-badge)

</div>

> [!IMPORTANT]
> ExoticGarden Legacy is an **unofficial community maintenance fork** for Slimefun Legacy, developed for use on **albionmc.com** while preserving the classic addon and its history.

## 🍎 What does ExoticGarden do?

ExoticGarden expands Slimefun with a large collection of **custom plants, fruit trees, crops, foods, ingredients, and crafting content**. It is one of the classic Slimefun addons and is also used by other addons as an optional source of food and plant materials.

The Legacy fork prioritizes continuity: existing item IDs, recipes, plants, trees, foods, schematics, plugin identity, and world data should continue to behave like classic ExoticGarden wherever practical.

## 🧪 Slimefun Legacy maintenance

Primary target:

- **Paper 26.2**
- **Java 25 runtime/build environment**
- **Slimefun Legacy**

The Bukkit plugin name intentionally remains **ExoticGarden**, preserving the `plugins/ExoticGarden` data folder, addon lookups, Slimefun registrations, and integrations that expect the classic plugin identity.

No GuizhanLibPlugin or GuguSlimefunLib dependency is required by this maintenance line.

See `COMPATIBILITY.md` for the detailed compatibility contract.

## 🛠️ Building

```bash
python3 scripts/prepare_legacy_1_1.py
python3 scripts/verify_legacy_1_1.py
mvn -B -ntp clean package
```

Expected output:

```text
target/ExoticGarden-Legacy-1.1.jar
```

## ❤️ Credits & project lineage

- **TheBusyBiscuit** — original creator of ExoticGarden and its classic gameplay/content.
- **Slimefun-Addon-Community/ExoticGarden** — community upstream repository and the immediate source from which this fork descends.
- **ExoticGarden community contributors** — years of maintenance, fixes, content, and compatibility work.
- **Slimefun developers and contributors** — for the platform and addon ecosystem ExoticGarden was built around.
- **wickidcow / Slimefun Legacy** — current preservation and modern-server maintenance for albionmc.com and the Slimefun Legacy ecosystem.

This repository exists to keep that work usable, not to claim ownership of the original project's creative history.

## 📜 GNU General Public License v3.0

ExoticGarden is licensed under the **GNU General Public License v3.0 (GPLv3)**. See `LICENSE` for the complete terms.

When distributing the plugin or a modified GPL-covered version, follow GPLv3 requirements, including preserving applicable notices, marking modified versions, licensing covered modified source under GPLv3, and providing the required Corresponding Source when conveying object code.

The software is provided **without warranty** as described by GPLv3.

## ⚖️ Independence & trademark notice

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

ExoticGarden Legacy, Slimefun Legacy, and this repository are independent community projects and are not sponsored, endorsed, approved, or operated by Mojang Studios or Microsoft. Minecraft-related names, brands, and assets remain the property of their respective rights holders.

This fork is likewise not represented as an official release of TheBusyBiscuit, the Slimefun-Addon-Community, or the original Slimefun team unless explicitly stated by those parties.

---

<div align="center">

**🍓 Plant the classics. Grow the legacy. 🌴**

</div>
