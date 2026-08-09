#!/usr/bin/env python3
"""
ExoticGarden Legacy 1.0 - Paper 26.2 source normalizer.

This script is intentionally small, deterministic and idempotent.
It updates only known legacy API usages inherited from the original
ExoticGarden source tree. It does NOT rename Slimefun item IDs,
plugin identity, recipes, plants, foods, or stored block data.

It can be run locally before committing the source changes, and the
GitHub Actions workflow also runs it before compiling as a safety net.
"""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java/io/github/thebusybiscuit/exoticgarden/ExoticGarden.java"
PLANTS = ROOT / "src/main/java/io/github/thebusybiscuit/exoticgarden/listeners/PlantsListener.java"


def fail(message: str) -> None:
    print(f"[ExoticGarden Legacy] ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"Required source file not found: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="\n")


def replace_known(text: str, old: str, new: str, expected_old: int | None, label: str) -> str:
    old_count = text.count(old)

    if old_count == 0:
        # Already modernized is valid.
        if new in text:
            print(f"[ExoticGarden Legacy] OK already modern: {label}")
            return text
        fail(f"Could not find legacy or modern form for: {label}")

    if expected_old is not None and old_count != expected_old:
        fail(f"Unexpected occurrence count for {label}: expected {expected_old}, found {old_count}")

    print(f"[ExoticGarden Legacy] Updating {label}: {old_count} occurrence(s)")
    return text.replace(old, new)


def modernize_main() -> None:
    text = read(MAIN)

    # This fork targets Paper directly, so the old "suggest Paper" call is unnecessary.
    text = replace_known(
        text,
        "import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;\n",
        "",
        1,
        "unused PaperLib import in ExoticGarden.java",
    )
    text = replace_known(
        text,
        "        PaperLib.suggestPaper(this);\n\n",
        "",
        1,
        "PaperLib.suggestPaper call",
    )

    # Never let the Legacy fork auto-update itself back to the abandoned upstream project.
    updater_import = "import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.GitHubBuildsUpdater;\n"
    if updater_import in text:
        text = text.replace(updater_import, "")
        print("[ExoticGarden Legacy] Removed legacy GitHubBuildsUpdater import")
    elif "GitHubBuildsUpdater" not in text:
        print("[ExoticGarden Legacy] OK updater import already removed")
    else:
        fail("Unexpected GitHubBuildsUpdater import layout")

    updater_pattern = re.compile(
        r'\n        // Auto Updater\n'
        r'        if \(cfg\.getBoolean\("options\.auto-update"\) && '
        r'getDescription\(\)\.getVersion\(\)\.startsWith\("DEV - "\)\) \{\n'
        r'            new GitHubBuildsUpdater\(this, getFile\(\), '
        r'"TheBusyBiscuit/ExoticGarden/master"\)\.start\(\);\n'
        r'        \}\n'
    )
    if updater_pattern.search(text):
        text = updater_pattern.sub("\n", text, count=1)
        print("[ExoticGarden Legacy] Removed abandoned upstream auto-updater")
    elif "options.auto-update" not in text and "GitHubBuildsUpdater" not in text:
        print("[ExoticGarden Legacy] OK updater block already removed")
    else:
        fail("Legacy auto-updater block changed unexpectedly; refusing a blind edit")

    # GRASS was renamed to SHORT_GRASS in modern Bukkit/Paper.
    text = replace_known(
        text,
        "new ItemStack(Material.GRASS)",
        "new ItemStack(Material.SHORT_GRASS)",
        4,
        "Material.GRASS recipe references",
    )

    # STEP_SOUND is deprecated for removal in 26.2. DESTROY_BLOCK takes BlockData.
    text = replace_known(
        text,
        "Effect.STEP_SOUND, Material.OAK_LEAVES",
        "Effect.DESTROY_BLOCK, Material.OAK_LEAVES.createBlockData()",
        2,
        "legacy block-break effects in ExoticGarden.java",
    )

    old_bug_url = 'return "https://github.com/TheBusyBiscuit/ExoticGarden/issues";'
    new_bug_url = 'return "https://github.com/wickidcow/ExoticGardenLegacy/issues";'
    text = replace_known(text, old_bug_url, new_bug_url, 1, "Legacy bug tracker URL")

    write(MAIN, text)


def modernize_plants_listener() -> None:
    text = read(PLANTS)

    text = replace_known(
        text,
        "== Material.GRASS)",
        "== Material.SHORT_GRASS)",
        1,
        "Material.GRASS harvest check",
    )

    text = replace_known(
        text,
        "Particle.VILLAGER_ANGRY",
        "Particle.ANGRY_VILLAGER",
        1,
        "renamed angry-villager particle",
    )

    text = replace_known(
        text,
        "Effect.STEP_SOUND, Material.OAK_LEAVES",
        "Effect.DESTROY_BLOCK, Material.OAK_LEAVES.createBlockData()",
        3,
        "legacy block-break effects in PlantsListener.java",
    )

    write(PLANTS, text)


def verify() -> None:
    combined = read(MAIN) + "\n" + read(PLANTS)

    forbidden = {
        "Material.GRASS)": "removed Material.GRASS API",
        "Particle.VILLAGER_ANGRY": "removed particle name",
        "Effect.STEP_SOUND": "deprecated STEP_SOUND effect",
        "GitHubBuildsUpdater": "abandoned upstream updater",
        "options.auto-update": "removed updater configuration hook",
        "TheBusyBiscuit/ExoticGarden/master": "abandoned upstream update target",
    }

    problems = []
    for token, reason in forbidden.items():
        if token in combined:
            problems.append(f"{reason}: {token}")

    if problems:
        fail("Foundation verification failed:\n  - " + "\n  - ".join(problems))

    print("[ExoticGarden Legacy] Paper 26.2 source normalization verified.")


if __name__ == "__main__":
    modernize_main()
    modernize_plants_listener()
    verify()
