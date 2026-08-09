#!/usr/bin/env python3
"""
ExoticGarden Legacy 1.0 - Paper 26.2 source normalizer.

This script is deterministic and idempotent. It updates inherited Bukkit/Paper
API usages across the COMPLETE Java source tree before compilation.

Foundation policy:
- Preserve the Bukkit plugin identity "ExoticGarden".
- Preserve every Slimefun item ID, recipe, plant, tree, food and data key.
- Do not add GuizhanLibPlugin or GuguSlimefunLib.
- Remove the abandoned upstream auto-updater.
- Modernize only known API compatibility points.

The GitHub Actions workflow runs this script before Maven compilation.
"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java"
MAIN = JAVA_ROOT / "io/github/thebusybiscuit/exoticgarden/ExoticGarden.java"


def fail(message: str) -> None:
    print(f"[ExoticGarden Legacy] ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def java_files() -> list[Path]:
    if not JAVA_ROOT.is_dir():
        fail(f"Java source tree not found: {JAVA_ROOT.relative_to(ROOT)}")

    files = sorted(JAVA_ROOT.rglob("*.java"))
    if not files:
        fail("No Java source files were found.")

    return files


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="\n")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def replace_all_java_literal(old: str, new: str, label: str) -> int:
    changed = 0
    occurrences = 0

    for path in java_files():
        text = read(path)
        count = text.count(old)

        if count:
            text = text.replace(old, new)
            write(path, text)
            changed += 1
            occurrences += count
            print(
                f"[ExoticGarden Legacy] {label}: "
                f"{relative(path)} ({count} occurrence(s))"
            )

    if occurrences == 0:
        print(f"[ExoticGarden Legacy] OK already modern: {label}")
    else:
        print(
            f"[ExoticGarden Legacy] Updated {label}: "
            f"{occurrences} occurrence(s) in {changed} file(s)"
        )

    return occurrences


def replace_material_grass() -> int:
    """
    Material.GRASS represented the short/tall decorative grass material in the
    inherited source. Paper 26.2 exposes SHORT_GRASS instead.

    The word-boundary regex deliberately does NOT touch GRASS_BLOCK,
    TALL_GRASS, LEGACY_GRASS, etc.
    """
    pattern = re.compile(r"\bMaterial\.GRASS\b")
    changed = 0
    occurrences = 0

    for path in java_files():
        text = read(path)
        new_text, count = pattern.subn("Material.SHORT_GRASS", text)

        if count:
            write(path, new_text)
            changed += 1
            occurrences += count
            print(
                f"[ExoticGarden Legacy] Material.GRASS -> Material.SHORT_GRASS: "
                f"{relative(path)} ({count} occurrence(s))"
            )

    if occurrences == 0:
        print("[ExoticGarden Legacy] OK already modern: Material.GRASS")
    else:
        print(
            f"[ExoticGarden Legacy] Updated Material.GRASS: "
            f"{occurrences} occurrence(s) in {changed} file(s)"
        )

    return occurrences


def replace_step_sound() -> int:
    """
    Paper 26.2 marks Effect.STEP_SOUND for removal and directs plugins to
    Effect.DESTROY_BLOCK. DESTROY_BLOCK expects BlockData, not Material.

    ExoticGarden's inherited calls pass direct Material enum constants, so
    convert:
        Effect.STEP_SOUND, Material.X
    into:
        Effect.DESTROY_BLOCK, Material.X.createBlockData()

    Any STEP_SOUND form not matching this known-safe shape is left untouched
    and caught by verification rather than being blindly rewritten.
    """
    pattern = re.compile(
        r"Effect\.STEP_SOUND\s*,\s*Material\.([A-Z0-9_]+)"
    )

    changed = 0
    occurrences = 0

    for path in java_files():
        text = read(path)

        def replacement(match: re.Match[str]) -> str:
            material = match.group(1)
            return (
                f"Effect.DESTROY_BLOCK, "
                f"Material.{material}.createBlockData()"
            )

        new_text, count = pattern.subn(replacement, text)

        if count:
            write(path, new_text)
            changed += 1
            occurrences += count
            print(
                f"[ExoticGarden Legacy] STEP_SOUND -> DESTROY_BLOCK: "
                f"{relative(path)} ({count} occurrence(s))"
            )

    if occurrences == 0:
        print("[ExoticGarden Legacy] OK already modern: Effect.STEP_SOUND")
    else:
        print(
            f"[ExoticGarden Legacy] Updated Effect.STEP_SOUND: "
            f"{occurrences} occurrence(s) in {changed} file(s)"
        )

    return occurrences


def modernize_main_plugin() -> None:
    if not MAIN.is_file():
        fail(f"Main plugin source not found: {relative(MAIN)}")

    text = read(MAIN)
    original = text

    # This fork targets Paper directly. The inherited PaperLib suggestion call
    # is no longer useful and needlessly couples startup to an old helper.
    text = text.replace(
        "import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;\n",
        "",
    )
    text = text.replace("        PaperLib.suggestPaper(this);\n\n", "")

    # Never allow ExoticGarden Legacy to update itself back to an abandoned
    # upstream build.
    text = text.replace(
        "import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.GitHubBuildsUpdater;\n",
        "",
    )

    updater_block = re.compile(
        r"\n\s*// Auto Updater\s*\n"
        r"\s*if\s*\(\s*cfg\.getBoolean\(\"options\.auto-update\"\)"
        r".*?"
        r"\n\s*\}\s*\n",
        flags=re.DOTALL,
    )
    text, updater_count = updater_block.subn("\n", text, count=1)

    # Point Slimefun's addon error reporting at the maintained fork.
    text = text.replace(
        'return "https://github.com/TheBusyBiscuit/ExoticGarden/issues";',
        'return "https://github.com/wickidcow/SF_ExoticGarden/issues";',
    )
    text = text.replace(
        'return "https://github.com/Slimefun-Addon-Community/ExoticGarden/issues";',
        'return "https://github.com/wickidcow/SF_ExoticGarden/issues";',
    )

    if text != original:
        write(MAIN, text)
        print(
            "[ExoticGarden Legacy] Updated main plugin compatibility hooks: "
            f"{relative(MAIN)}"
        )
    else:
        print("[ExoticGarden Legacy] OK main plugin compatibility hooks already modern")


def verify_java_tree() -> None:
    """
    Fail BEFORE Maven if a known Paper 26.2-incompatible API survived.
    This is intentionally tree-wide so future inherited files cannot be missed.
    """
    forbidden_patterns = [
        (
            re.compile(r"\bMaterial\.GRASS\b"),
            "Material.GRASS remains; Paper 26.2 uses Material.SHORT_GRASS",
        ),
        (
            re.compile(r"\bParticle\.VILLAGER_ANGRY\b"),
            "Particle.VILLAGER_ANGRY remains; use Particle.ANGRY_VILLAGER",
        ),
        (
            re.compile(r"\bEffect\.STEP_SOUND\b"),
            "Effect.STEP_SOUND remains; Paper 26.2 uses Effect.DESTROY_BLOCK",
        ),
        (
            re.compile(r"\bGitHubBuildsUpdater\b"),
            "Legacy upstream GitHubBuildsUpdater remains",
        ),
        (
            re.compile(r"options\.auto-update"),
            "Legacy upstream auto-update configuration hook remains",
        ),
        (
            re.compile(r"TheBusyBiscuit/ExoticGarden/master"),
            "Abandoned ExoticGarden upstream update target remains",
        ),
    ]

    problems: list[str] = []

    for path in java_files():
        text = read(path)

        for pattern, reason in forbidden_patterns:
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                problems.append(f"{relative(path)}:{line}: {reason}")

    if problems:
        fail(
            "Paper 26.2 compatibility verification failed:\n  - "
            + "\n  - ".join(problems)
        )

    print(
        f"[ExoticGarden Legacy] Verified {len(java_files())} Java source files: "
        "no known Foundation 1.0 Paper 26.2 blockers remain."
    )


def verify_dependency_policy() -> None:
    plugin_yml = ROOT / "src/main/resources/plugin.yml"

    if not plugin_yml.is_file():
        fail("src/main/resources/plugin.yml is missing")

    text = read(plugin_yml)

    declared_bad_dependencies = [
        "\n  - GuizhanLibPlugin",
        "\n- GuizhanLibPlugin",
        "\n  - GuguSlimefunLib",
        "\n- GuguSlimefunLib",
    ]

    for token in declared_bad_dependencies:
        if token in text:
            fail(
                "Foundation dependency policy violation in plugin.yml: "
                f"{token.strip()}"
            )

    print(
        "[ExoticGarden Legacy] Dependency policy verified: "
        "no GuizhanLibPlugin or GuguSlimefunLib hard dependency."
    )


def main() -> None:
    print("[ExoticGarden Legacy] Preparing complete source tree for Paper 26.2...")

    modernize_main_plugin()

    replace_material_grass()

    replace_all_java_literal(
        "Particle.VILLAGER_ANGRY",
        "Particle.ANGRY_VILLAGER",
        "Particle.VILLAGER_ANGRY -> Particle.ANGRY_VILLAGER",
    )

    # Run this AFTER Material.GRASS conversion so GrassSeeds becomes
    # Material.SHORT_GRASS.createBlockData() automatically.
    replace_step_sound()

    verify_java_tree()
    verify_dependency_policy()

    print("[ExoticGarden Legacy] Foundation 1.0 Paper 26.2 normalization complete.")


if __name__ == "__main__":
    main()
