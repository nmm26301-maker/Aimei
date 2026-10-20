import importlib.util
import stat
import tempfile
import unittest
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "source_import", Path(__file__).with_name("import_android_source.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SourceImportTests(unittest.TestCase):
    def test_nested_android_project(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "source.zip"
            with zipfile.ZipFile(archive, "w") as bundle:
                bundle.writestr("aimei/settings.gradle", "include ':app'")
                bundle.writestr("aimei/app/src/main/AndroidManifest.xml", "<manifest/>")
            module.safe_extract(archive, root / "extracted")
            self.assertEqual(module.find_project(root / "extracted"),
                             root / "extracted/aimei")

    def test_traversal_is_rejected_before_extraction(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "source.zip"
            with zipfile.ZipFile(archive, "w") as bundle:
                bundle.writestr("valid.txt", "valid")
                bundle.writestr("../escape.txt", "invalid")
            with self.assertRaises(ValueError):
                module.safe_extract(archive, root / "extracted")
            self.assertFalse((root / "escape.txt").exists())
            self.assertFalse((root / "extracted/valid.txt").exists())

    def test_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "source.zip"
            with zipfile.ZipFile(archive, "w") as bundle:
                item = zipfile.ZipInfo("link")
                item.create_system = 3
                item.external_attr = (stat.S_IFLNK | 0o777) << 16
                bundle.writestr(item, "../outside")
            with self.assertRaises(ValueError):
                module.safe_extract(archive, root / "extracted")

    def test_multiple_projects_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ("first", "second"):
                (root / name).mkdir()
                (root / name / "settings.gradle").write_text("")
            with self.assertRaises(ValueError):
                module.find_project(root)

    def test_generated_build_projects_are_ignored(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "settings.gradle.kts").write_text("")
            (root / "build/nested").mkdir(parents=True)
            (root / "build/nested/settings.gradle").write_text("")
            self.assertEqual(module.find_project(root), root)

    def test_no_project(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertIsNone(module.find_project(Path(directory)))


if __name__ == "__main__":
    unittest.main()
