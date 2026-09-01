#!/usr/bin/env python3
"""
Deterministic source normalization for the Slimefun Legacy ExoticGarden fork.

The normalizer keeps one source tree compilable on the supported Paper/Purpur
range (Minecraft 1.21.11 through 26.2) while removing abandoned updater hooks
and fork-specific dependencies.
"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java"
MAIN = JAVA_ROOT / "io/github/thebusybiscuit/exoticgarden/ExoticGarden.java"
COMPAT = "io.github.thebusybiscuit.exoticgarden.compat.RuntimeCompatibility"


def fail(message: str) -> None:
    print(f"[ExoticGarden Legacy] ERROR: {message}", file=sys.stderr)
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
            print(f"[compat] {label}: {rel(path)} ({count})")
    if total:
        print(f"[compat] Updated {label}: {total} occurrence(s), {touched} file(s)")
    else:
        print(f"[compat] OK already normalized: {label}")
    return total


def replace_literal(old: str, new: str, label: str) -> int:
    return replace_regex(re.compile(re.escape(old)), lambda _: new, label)


def block_effect_replacement(match: re.Match[str]) -> str:
    world = match.group("world")
    location = match.group("location").strip()
    material = match.group("material")
    return f"{COMPAT}.playBlockBreakEffect({world}, {location}, Material.{material});"


def modernize_removed_paper_apis() -> None:
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

    legacy_effect = re.compile(
        r"(?P<world>[A-Za-z0-9_().]+)\.playEffect\(\s*"
        r"(?P<location>[^,\n]+),\s*Effect\.STEP_SOUND,\s*"
        r"Material\.(?P<material>[A-Z0-9_]+)\s*\);",
        re.MULTILINE,
    )
    modern_effect = re.compile(
        r"(?P<world>[A-Za-z0-9_().]+)\.playEffect\(\s*"
        r"(?P<location>[^,\n]+),\s*Effect\.DESTROY_BLOCK,\s*"
        r"Material\.(?P<material>[A-Z0-9_]+)\.createBlockData\(\)\s*\);",
        re.MULTILINE,
    )

    replace_regex(
        legacy_effect,
        block_effect_replacement,
        "Effect.STEP_SOUND -> cross-version block effect helper",
    )
    replace_regex(
        modern_effect,
        block_effect_replacement,
        "Effect.DESTROY_BLOCK -> cross-version block effect helper",
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
    text = text.replace(
        "import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;\n",
        "",
    )
    text = text.replace("        PaperLib.suggestPaper(this);\n", "")

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
        print("[compat] Main plugin bootstrap/updater normalized.")
    else:
        print("[compat] Main plugin bootstrap already normalized.")


def verify_no_known_blockers() -> None:
    forbidden = [
        (re.compile(r"\bMaterial\.GRASS\b"), "removed Material.GRASS"),
        (re.compile(r"\bParticle\.VILLAGER_ANGRY\b"), "removed Particle.VILLAGER_ANGRY"),
        (re.compile(r"\bEffect\.STEP_SOUND\b"), "version-specific Effect.STEP_SOUND"),
        (re.compile(r"\bEffect\.DESTROY_BLOCK\b"), "version-specific Effect.DESTROY_BLOCK"),
        (re.compile(r"\bGitHubBuildsUpdater\b"), "abandoned GitHubBuildsUpdater"),
        (re.compile(r"TheBusyBiscuit/ExoticGarden/master"), "abandoned ExoticGarden update target"),
        (re.compile(r"com\.xzavier0722\.mc\.plugin\.slimefun4"), "Gugu-only storage/API import"),
        (re.compile(r"net\.guizhanss"), "Guizhan-only API import"),
    ]

    problems: list[str] = []
    for path in java_files():
        text = read(path)
        for pattern, reason in forbidden:
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                problems.append(f"{rel(path)}:{line}: {reason}")

    if problems:
        fail("Known compatibility blockers remain:\n  - " + "\n  - ".join(problems))

    print(
        f"[compat] Verified {len(java_files())} Java files: "
        "no known cross-version/fork compatibility blocker remains."
    )


def main() -> None:
    print("[ExoticGarden Legacy] Preparing source tree...")
    modernize_removed_paper_apis()
    remove_abandoned_updater()
    verify_no_known_blockers()
    print("[ExoticGarden Legacy] Source preparation complete.")


if __name__ == "__main__":
    main()
