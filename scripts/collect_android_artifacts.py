"""Collect only real Gradle APK outputs and record their checksums."""
import hashlib
import os
import shutil
import subprocess
import zipfile
from pathlib import Path

project = Path(os.environ["PROJECT_DIR"]).resolve()
destination = Path(os.environ["GITHUB_WORKSPACE"]) / "dist"
destination.mkdir(exist_ok=True)
apks = sorted(project.glob("**/build/outputs/apk/debug/*.apk"))
if not apks:
    raise SystemExit("Build did not produce a debug APK")
checksums = []
for index, path in enumerate(apks):
    name = "Aimei-0.6.0.apk" if len(apks) == 1 else f"Aimei-0.6.0-{index + 1}.apk"
    output = destination / name
    shutil.copy2(path, output)
    tools = sorted((Path(os.environ["ANDROID_HOME"]) / "build-tools").glob("*/apksigner"))
    if not tools:
        raise SystemExit("APK signature verification tool not found")
    subprocess.run([str(tools[-1]), "verify", "--verbose", "--print-certs", str(output)], check=True)
    checksums.append(hashlib.sha256(output.read_bytes()).hexdigest() + "  " + name)
(destination / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8")
(destination / "BUILD-INFO.txt").write_text(
    "Application: Aimei\n"
    "Variant: debug, Android test signing\n"
    "Commit: " + os.environ["GITHUB_SHA"] + "\n"
    "Anywear realtime try-on: not verified on a physical phone by this workflow\n",
    encoding="utf-8")
with zipfile.ZipFile(destination / "Aimei-built-source.zip", "w",
                     compression=zipfile.ZIP_DEFLATED) as bundle:
    for path in sorted(project.rglob("*")):
        if path.is_symlink() or not path.is_file():
            continue
        relative = path.relative_to(project)
        if any(part in {".git", ".gradle", "build", "node_modules"} for part in relative.parts):
            continue
        if path.name == "local.properties" or path.suffix.lower() in {".jks", ".keystore"}:
            continue
        bundle.write(path, relative)
with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as stream:
    stream.write("## Android debug APK built\n\n"
                 "Compilation and Android lint passed. Download the Aimei-Android-debug artifact.\n\n"
                 "This is a test-signed build. Camera permission, Anywear connection and live "
                 "garment rendering still require physical Android device verification.\n\n")
    stream.write("SHA256:\n\n" + "\n".join(checksums) + "\n")
print("Collected", len(apks), "APK(s)")
