PRAGMA foreign_keys = ON;

CREATE TABLE metadata (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT
);

CREATE TABLE books (
    book_key TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    short_name TEXT,
    testament TEXT NOT NULL,
    canonical_order INTEGER NOT NULL,
    chapter_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE chapters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_key TEXT NOT NULL,
    chapter_number INTEGER NOT NULL,
    UNIQUE(book_key, chapter_number),
    FOREIGN KEY(book_key) REFERENCES books(book_key)
);

CREATE TABLE verses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stable_id TEXT NOT NULL UNIQUE,
    chapter_id INTEGER NOT NULL,
    verse_label TEXT NOT NULL,
    verse_order INTEGER NOT NULL,
    text TEXT NOT NULL,
    is_heading INTEGER NOT NULL DEFAULT 0,
    paragraph_start INTEGER NOT NULL DEFAULT 0,
    UNIQUE(chapter_id, verse_order),
    FOREIGN KEY(chapter_id) REFERENCES chapters(id)
);

CREATE TABLE footnotes (
    footnote_id TEXT PRIMARY KEY NOT NULL,
    verse_id TEXT NOT NULL,
    marker TEXT,
    text TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'editorial',
    FOREIGN KEY(verse_id) REFERENCES verses(stable_id)
);

CREATE TABLE cross_references (
    reference_id TEXT PRIMARY KEY NOT NULL,
    source_verse_id TEXT NOT NULL,
    target_book_key TEXT NOT NULL,
    target_chapter INTEGER NOT NULL,
    target_verse_start TEXT NOT NULL,
    target_verse_end TEXT,
    FOREIGN KEY(source_verse_id) REFERENCES verses(stable_id)
);

CREATE TABLE tokens (
    token_id TEXT PRIMARY KEY NOT NULL,
    verse_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    surface TEXT NOT NULL,
    language TEXT,
    lemma TEXT,
    strong_id TEXT,
    morphology TEXT,
    source_dataset TEXT,
    UNIQUE(verse_id, position),
    FOREIGN KEY(verse_id) REFERENCES verses(stable_id)
);

CREATE INDEX idx_chapters_book ON chapters(book_key, chapter_number);
CREATE INDEX idx_verses_chapter ON verses(chapter_id, verse_order);
CREATE INDEX idx_verses_text ON verses(text);
CREATE INDEX idx_tokens_verse ON tokens(verse_id, position);
CREATE INDEX idx_xrefs_source ON cross_references(source_verse_id);
