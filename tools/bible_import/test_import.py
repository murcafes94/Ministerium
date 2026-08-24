#!/usr/bin/env python3
import sqlite3
import tempfile
import unittest
import zipfile
from pathlib import Path

import import_bible


class ImportBibleTests(unittest.TestCase):
    def build(self, source: Path, fmt: str):
        out = source.parent / "test.bible"
        metadata = {
            "edition_id": "test-es",
            "name": "Test Bible",
            "abbreviation": "TST",
            "language": "es",
            "canon": "catholic",
            "source_format": fmt,
            "version": "1",
            "copyright_notice": "synthetic test data",
            "license_id": "CC0-test",
            "content_hash": "",
            "min_app_version": "3.1.0",
        }
        builder = import_bible.Builder(out, Path(import_bible.__file__).with_name("schema.sql"), metadata)
        getattr(import_bible, "import_" + fmt)(builder, source)
        builder.finish()
        return out

    def assert_john(self, db_path: Path):
        db = sqlite3.connect(str(db_path))
        try:
            rows = db.execute(
                "SELECT b.book_key,c.chapter_number,v.verse_label,v.text "
                "FROM verses v JOIN chapters c ON c.id=v.chapter_id "
                "JOIN books b ON b.book_key=c.book_key ORDER BY v.verse_order"
            ).fetchall()
            self.assertEqual("JHN", rows[0][0])
            self.assertEqual(1, rows[0][1])
            self.assertEqual("1", rows[0][2])
            self.assertIn("Principio", rows[0][3])
        finally:
            db.close()

    def test_usfm(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "JHN.usfm"
            p.write_text("\\id JHN\n\\h Juan\n\\c 1\n\\p\n\\v 1 En el Principio era la Palabra.\n\\v 2 Ella estaba al principio.\n", encoding="utf-8")
            self.assert_john(self.build(p, "usfm"))

    def test_osis(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "test.xml"
            p.write_text("""<?xml version='1.0'?><osis><osisText><div type='book' osisID='John'><chapter osisID='John.1'><verse osisID='John.1.1'>En el Principio era la Palabra.</verse></chapter></div></osisText></osis>""", encoding="utf-8")
            self.assert_john(self.build(p, "osis"))

    def test_usx(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "JHN.usx"
            p.write_text("""<?xml version='1.0'?><usx version='3.0'><book code='JHN'>Juan</book><chapter number='1' style='c'/><para style='p'><verse number='1' style='v'/>En el Principio era la Palabra.<verse eid='JHN 1:1'/></para></usx>""", encoding="utf-8")
            self.assert_john(self.build(p, "usx"))

    def test_epub_semantic_ids(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "test.epub"
            with zipfile.ZipFile(p, "w") as zf:
                zf.writestr("OEBPS/john.xhtml", "<html><body><p id='JHN.1.1'>En el Principio era la Palabra.</p></body></html>")
            self.assert_john(self.build(p, "epub"))


if __name__ == "__main__":
    unittest.main()
