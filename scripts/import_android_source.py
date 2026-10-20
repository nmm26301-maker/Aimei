"""Import the existing Aimei Android project without changing repository files."""
import os
import re
import shutil
import stat
import zipfile
from pathlib import Path, PurePosixPath

EXCLUDED = {".git", ".gradle", "build", "node_modules", "__MACOSX", ".github"}


def safe_extract(archive, destination):
    destination = Path(destination).resolve()
    with zipfile.ZipFile(archive) as bundle:
        entries = bundle.infolist()
        if len(entries) > 10000 or sum(x.file_size for x in entries) > 200_000_000:
            raise ValueError("Source archive is too large")
        for entry in entries:
            path = PurePosixPath(entry.filename)
            mode = entry.external_attr >> 16
            if (path.is_absolute() or ".." in path.parts or "\\" in entry.filename
                    or stat.S_ISLNK(mode) or "\n" in entry.filename or "\r" in entry.filename):
                raise ValueError("Unsafe archive path: " + repr(entry.filename))
            target = (destination / entry.filename).resolve()
            if not target.is_relative_to(destination):
                raise ValueError("Archive path escapes extraction directory")
        bundle.extractall(destination)


def find_project(base):
    candidates = set()
    for name in ("settings.gradle", "settings.gradle.kts"):
        for path in base.rglob(name):
            relative = path.relative_to(base)
            if not any(part in EXCLUDED for part in relative.parts):
                candidates.add(path.parent)
    if len(candidates) > 1:
        raise ValueError("Multiple Android projects found; retain one source project")
    return next(iter(candidates), None)


def emit(**values):
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as stream:
            for key, value in values.items():
                if "\n" in str(value) or "\r" in str(value):
                    raise ValueError("Invalid workflow output")
                stream.write(f"{key}={value}\n")


def main():
    workspace = Path(os.environ.get("GITHUB_WORKSPACE", ".")).resolve()
    target = Path(os.environ["RUNNER_TEMP"]) / "aimei-source"
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    original = find_project(workspace)
    if original:
        shutil.copytree(original, target / "project",
                        ignore=shutil.ignore_patterns(*EXCLUDED))
    else:
        exact = workspace / "Aimei_Anywear_Android_v0.5.0_Source.zip"
        archives = [exact] if exact.is_file() else list(workspace.glob("Aimei*Source.zip"))
        if not archives:
            emit(ready="false")
            summary = os.environ.get("GITHUB_STEP_SUMMARY")
            if summary:
                with open(summary, "a", encoding="utf-8") as stream:
                    stream.write("## Waiting for Android source\n\n"
                                 "Upload Aimei_Anywear_Android_v0.5.0_Source.zip "
                                 "to the repository root. No APK was built.\n")
            print("Waiting for source ZIP; no APK was built.")
            return
        if len(archives) != 1:
            raise ValueError("Multiple source ZIPs found; retain one source ZIP")
        safe_extract(archives[0], target)
    project = find_project(target)
    if project is None:
        raise ValueError("Android settings.gradle or settings.gradle.kts not found")
    manifests = list(project.glob("*/src/main/AndroidManifest.xml"))
    if not manifests:
        raise ValueError("No Android application manifest found")
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        raise ValueError("Android SDK is unavailable")
    (project / "local.properties").write_text("sdk.dir=" + sdk + "\n", encoding="utf-8")
    version = "8.10.2"
    wrapper = project / "gradle/wrapper/gradle-wrapper.properties"
    if wrapper.exists():
        match = re.search(r"gradle-(\d+(?:\.\d+){1,2})-(?:bin|all)\.zip",
                          wrapper.read_text(encoding="utf-8"))
        if match:
            version = match.group(1)
    emit(ready="true", directory=project, gradle_version=version)
    print("Imported Android project:", project)
    print("Gradle version:", version)


if __name__ == "__main__":
    main()
