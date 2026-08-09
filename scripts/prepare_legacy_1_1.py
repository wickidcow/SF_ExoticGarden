#!/usr/bin/env python3
"""
ExoticGarden Legacy 1.1 source preparation.

This is not a patch file. It is a deterministic compatibility normalizer which
guards the inherited ExoticGarden source tree against a small set of known
Paper 26.2 removals and abandoned-upstream hooks.

The important 1.1 gameplay listener is supplied as a complete drop-in Java file.
This script handles the remaining inherited classes (for example GrassSeeds and
ExoticGardenRecipeTypes) so old enum names cannot reappear unnoticed.
"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java"
MAIN = JAVA_ROOT / "io/github/thebusybiscuit/exoticgarden/ExoticGarden.java"


def fail(message: str) -> None:
    print(f"[ExoticGarden Legacy 1.1] ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def java_files() -> list[Path]:
    if not JAVA_ROOT.is_dir():
        fail(f"Missing Java source tree: {JAVA_ROOT.relative_to(ROOT)}")

    result = sorted(JAVA_ROOT.rglob("*.java"))

    if not result:
        fail("No Java source files found.")

    return result


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="\n")


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def replace_regex(pattern: re.Pattern[str], replacement, label: str) -> int:
    total = 0
    touched = 0

    for path in java_files():
        original = read(path)
        updated, count = pattern.subn(replacement, original)

        if count:
            write(path, updated)
            total += count
            touched += 1
            print(f"[1.1] {label}: {rel(path)} ({count})")

    if total:
        print(f"[1.1] Updated {label}: {total} occurrence(s), {touched} file(s)")
    else:
        print(f"[1.1] OK already modern: {label}")

    return total


def replace_literal(old: str, new: str, label: str) -> int:
    return replace_regex(re.compile(re.escape(old)), lambda _: new, label)


def modernize_removed_paper_apis() -> None:
    # Exact word boundary protects GRASS_BLOCK, TALL_GRASS, etc.
    replace_regex(
        re.compile(r"\bMaterial\.GRASS\b"),
        lambda _: "Material.SHORT_GRASS",
        "Material.GRASS -> Material.SHORT_GRASS",
    )

    replace_literal(
        "Particle.VILLAGER_ANGRY",
        "Particle.ANGRY_VILLAGER",
        "Particle.VILLAGER_ANGRY -> Particle.ANGRY_VILLAGER",
    )

    # DESTROY_BLOCK requires BlockData. Only rewrite the known old
    # Effect.STEP_SOUND + direct Material payload shape.
    step_pattern = re.compile(
        r"Effect\.STEP_SOUND\s*,\s*Material\.([A-Z0-9_]+)"
    )

    def step_replacement(match: re.Match[str]) -> str:
        material = match.group(1)
        return (
            "Effect.DESTROY_BLOCK, "
            f"Material.{material}.createBlockData()"
        )

    replace_regex(
        step_pattern,
        step_replacement,
        "Effect.STEP_SOUND -> Effect.DESTROY_BLOCK(BlockData)",
    )


def remove_abandoned_updater() -> None:
    if not MAIN.is_file():
        fail(f"Missing main plugin source: {rel(MAIN)}")

    text = read(MAIN)
    original = text

    text = text.replace(
        "import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.GitHubBuildsUpdater;\n",
        "",
    )

    # The project now requires Paper, so the old "suggest Paper" helper is not
    # useful. Remove it only from the main plugin bootstrap.
    text = text.replace(
        "import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;\n",
        "",
    )
    text = text.replace("        PaperLib.suggestPaper(this);\n", "")

    # Remove the exact historical updater block regardless of whitespace.
    text = re.sub(
        r"\n\s*// Auto Updater\s*\n"
        r"\s*if\s*\(\s*cfg\.getBoolean\(\"options\.auto-update\"\).*?"
        r"GitHubBuildsUpdater.*?\n\s*\}\s*",
        "\n",
        text,
        count=1,
        flags=re.DOTALL,
    )

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
        print("[1.1] Main plugin bootstrap/updater normalized.")
    else:
        print("[1.1] Main plugin bootstrap already normalized.")


def verify_no_known_blockers() -> None:
    forbidden = [
        (
            re.compile(r"\bMaterial\.GRASS\b"),
            "removed Material.GRASS",
        ),
        (
            re.compile(r"\bParticle\.VILLAGER_ANGRY\b"),
            "removed Particle.VILLAGER_ANGRY",
        ),
        (
            re.compile(r"\bEffect\.STEP_SOUND\b"),
            "deprecated/removal-target Effect.STEP_SOUND",
        ),
        (
            re.compile(r"\bGitHubBuildsUpdater\b"),
            "abandoned GitHubBuildsUpdater",
        ),
        (
            re.compile(r"TheBusyBiscuit/ExoticGarden/master"),
            "abandoned ExoticGarden update target",
        ),
        (
            re.compile(r"com\.xzavier0722\.mc\.plugin\.slimefun4"),
            "Gugu-only storage/API import",
        ),
        (
            re.compile(r"net\.guizhanss"),
            "Guizhan-only API import",
        ),
    ]

    problems: list[str] = []

    for path in java_files():
        text = read(path)

        for pattern, reason in forbidden:
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                problems.append(f"{rel(path)}:{line}: {reason}")

    if problems:
        fail(
            "Known compatibility blockers remain:\n  - "
            + "\n  - ".join(problems)
        )

    print(
        f"[1.1] Verified {len(java_files())} Java files: "
        "no known Paper/Gugu/Guizhan compatibility blocker remains."
    )


def main() -> None:
    print("[ExoticGarden Legacy 1.1] Preparing source tree...")
    modernize_removed_paper_apis()
    remove_abandoned_updater()
    verify_no_known_blockers()
    print("[ExoticGarden Legacy 1.1] Source preparation complete.")


if __name__ == "__main__":
    main()
