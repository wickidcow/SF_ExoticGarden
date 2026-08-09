#!/usr/bin/env python3
"""
Static compatibility verification for ExoticGarden Legacy 1.1.

The goal is to prevent accidental regression back to:
- removed Paper 26.2 API names,
- asynchronous world/block mutation in PlantsListener,
- hard Guizhan/Gugu dependencies,
- renamed Bukkit plugin identity,
- wrong release artifact naming,
- abandoned upstream self-updating.
"""

from __future__ import annotations

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java"
PLUGIN_YML = ROOT / "src/main/resources/plugin.yml"
POM = ROOT / "pom.xml"
PLANTS = (
    JAVA_ROOT
    / "io/github/thebusybiscuit/exoticgarden/listeners/PlantsListener.java"
)


def fail(items: list[str]) -> None:
    print("[ExoticGarden Legacy 1.1] VERIFICATION FAILED", file=sys.stderr)
    for item in items:
        print(f"  - {item}", file=sys.stderr)
    raise SystemExit(1)


def require(condition: bool, message: str, problems: list[str]) -> None:
    if not condition:
        problems.append(message)


def scan_java(problems: list[str]) -> None:
    files = sorted(JAVA_ROOT.rglob("*.java"))
    require(bool(files), "No Java source files found.", problems)

    forbidden = {
        r"\bMaterial\.GRASS\b": "Material.GRASS survived normalization",
        r"\bParticle\.VILLAGER_ANGRY\b": "old angry-villager particle survived",
        r"\bEffect\.STEP_SOUND\b": "STEP_SOUND survived normalization",
        r"\bGitHubBuildsUpdater\b": "abandoned upstream updater survived",
        r"com\.xzavier0722\.mc\.plugin\.slimefun4": "Gugu-only Java API import found",
        r"net\.guizhanss": "Guizhan-only Java API import found",
    }

    for path in files:
        text = path.read_text(encoding="utf-8")

        for pattern, label in forbidden.items():
            if re.search(pattern, text):
                problems.append(
                    f"{path.relative_to(ROOT).as_posix()}: {label}"
                )


def verify_listener(problems: list[str]) -> None:
    require(PLANTS.is_file(), "Replacement PlantsListener.java is missing.", problems)

    if not PLANTS.is_file():
        return

    text = PLANTS.read_text(encoding="utf-8")

    require(
        "BlockStorageCompat" in text,
        "PlantsListener is not using the shared BlockStorage compatibility boundary.",
        problems,
    )
    require(
        "RuntimeCompatibility.logStartup" in text,
        "Runtime compatibility diagnostics are not wired into listener startup.",
        problems,
    )
    require(
        "options.auto-generate-plants" in text,
        "Natural-generation enable/disable option is not enforced.",
        problems,
    )
    require(
        "compatibility.protect-piston-movement" in text,
        "Piston storage-safety option is not enforced.",
        problems,
    )
    require(
        "BlockPistonExtendEvent" in text and "BlockPistonRetractEvent" in text,
        "Piston movement safety listeners are missing.",
        problems,
    )

    # These patterns were the main threading/chunk-generation hazards in the
    # inherited listener.
    require(
        "getChunkAtAsync" not in text,
        "PlantsListener still contains asynchronous chunk acquisition.",
        problems,
    )
    require(
        ".thenRun(" not in text,
        "PlantsListener still mutates world state from async completion callbacks.",
        problems,
    )
    require(
        "PaperLib" not in text,
        "PlantsListener still depends on PaperLib async helpers.",
        problems,
    )

    require(
        "EventPriority.MONITOR" not in text,
        "PlantsListener still mutates/cancels events at MONITOR priority.",
        problems,
    )
    require(
        "world.getWorldBorder().getSize() / 2.0D" in text,
        "World-border radius handling is missing.",
        problems,
    )
    require(
        "world.getWorldBorder().getCenter()" in text,
        "Moved world-border center handling is missing.",
        problems,
    )
    require(
        "world.getHighestBlockYAt" in text,
        "Modern highest-block generation scan is missing.",
        problems,
    )
    require(
        "world.getMinHeight()" in text,
        "Modern minimum-world-height guard is missing.",
        problems,
    )


def verify_plugin_yml(problems: list[str]) -> None:
    require(PLUGIN_YML.is_file(), "plugin.yml is missing.", problems)

    if not PLUGIN_YML.is_file():
        return

    text = PLUGIN_YML.read_text(encoding="utf-8")

    require(
        re.search(r"(?m)^name:\s*ExoticGarden\s*$", text) is not None,
        "Bukkit plugin identity must remain exactly 'ExoticGarden'.",
        problems,
    )
    require(
        "main: io.github.thebusybiscuit.exoticgarden.ExoticGarden" in text,
        "Original ExoticGarden main class identity changed.",
        problems,
    )
    require(
        re.search(r"(?m)^\s*-\s*Slimefun\s*$", text) is not None,
        "Slimefun hard dependency is missing.",
        problems,
    )

    # Comments may name optional libraries, so only reject actual YAML list
    # dependency entries.
    require(
        re.search(r"(?m)^\s*-\s*GuizhanLibPlugin\s*$", text) is None,
        "GuizhanLibPlugin must not be a declared dependency.",
        problems,
    )
    require(
        re.search(r"(?m)^\s*-\s*GuguSlimefunLib\s*$", text) is None,
        "GuguSlimefunLib must not be a declared dependency.",
        problems,
    )


def verify_pom(problems: list[str]) -> None:
    require(POM.is_file(), "pom.xml is missing.", problems)

    if not POM.is_file():
        return

    try:
        tree = ET.parse(POM)
    except ET.ParseError as ex:
        problems.append(f"pom.xml is invalid XML: {ex}")
        return

    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}

    version = root.findtext("m:version", namespaces=ns)
    final_name = root.findtext("m:build/m:finalName", namespaces=ns)

    require(version == "1.1", f"Maven version must be 1.1 (found {version!r}).", problems)
    require(
        final_name == "ExoticGarden-Legacy-1.1",
        f"finalName must be ExoticGarden-Legacy-1.1 (found {final_name!r}).",
        problems,
    )

    xml = POM.read_text(encoding="utf-8")

    require(
        "<artifactId>Slimefun4</artifactId>" in xml,
        "Public Slimefun4 compile baseline is missing.",
        problems,
    )
    require(
        "<slimefun.version>RC-37</slimefun.version>" in xml,
        "Expected common Slimefun RC-37 API baseline is missing.",
        problems,
    )
    require(
        "SlimefunGuguProject" not in xml,
        "pom.xml directly binds to the Gugu implementation.",
        problems,
    )
    require(
        "Slimefun-United" not in xml,
        "pom.xml directly binds to the United implementation.",
        problems,
    )
    require(
        "GuizhanLib" not in xml,
        "pom.xml directly binds to GuizhanLib.",
        problems,
    )


def build_registration_manifest() -> None:
    """
    Emit a deterministic registration manifest for release diagnostics.

    ExoticGarden creates many IDs dynamically from berry/tree/plant names, so
    this is intentionally diagnostic rather than a brittle hand-written list.
    It still makes accidental explicit-ID changes visible in every CI build.
    """
    output = ROOT / "target/exoticgarden-explicit-id-manifest.txt"
    output.parent.mkdir(parents=True, exist_ok=True)

    ids: set[str] = set()
    pattern = re.compile(r'new\s+SlimefunItemStack\s*\(\s*"([A-Z0-9_]+)"')

    for path in JAVA_ROOT.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        ids.update(pattern.findall(text))

    output.write_text(
        "\n".join(sorted(ids)) + ("\n" if ids else ""),
        encoding="utf-8",
    )

    print(
        f"[ExoticGarden Legacy 1.1] Explicit-ID manifest: "
        f"{len(ids)} ID(s) -> {output.relative_to(ROOT)}"
    )


def main() -> None:
    problems: list[str] = []

    scan_java(problems)
    verify_listener(problems)
    verify_plugin_yml(problems)
    verify_pom(problems)

    if problems:
        fail(problems)

    build_registration_manifest()

    print("[ExoticGarden Legacy 1.1] Static compatibility verification passed.")


if __name__ == "__main__":
    main()
