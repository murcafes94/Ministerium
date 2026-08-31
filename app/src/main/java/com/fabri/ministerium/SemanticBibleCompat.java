package com.fabri.ministerium;

import android.content.Context;
import android.text.TextUtils;

import org.ministerium.bible.semantic.BibleBook;
import org.ministerium.bible.semantic.BibleEdition;
import org.ministerium.bible.semantic.BibleVerse;
import org.ministerium.bible.semantic.SqliteBibleRepository;

import java.io.File;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Compatibility bridge between the current EPUB-based Bible reader and the
 * semantic SQLite packages introduced in Ministerium 3.1.
 *
 * The reader may call this first; returning null means "use the existing EPUB
 * pipeline". No Bible text is embedded in this class.
 */
public final class SemanticBibleCompat {
    private SemanticBibleCompat() {}

    public static String chapterHtml(Context context, BibleRepository.Book legacyBook,
                                     int chapterNumber) {
        File packageFile = findInstalledPackage(context);
        if (packageFile == null || legacyBook == null || chapterNumber < 1) return null;

        try (SqliteBibleRepository repository = SqliteBibleRepository.openReadOnly(packageFile)) {
            BibleBook semanticBook = matchBook(repository.listBooks(), legacyBook);
            if (semanticBook == null) return null;
            List<BibleVerse> verses = repository.getChapter(semanticBook.getBookId(), chapterNumber);
            if (verses == null || verses.isEmpty()) return null;
            return render(repository.getEdition(), semanticBook, chapterNumber, verses);
        } catch (Exception ignored) {
            // A malformed/incompatible semantic package must never break the
            // current EPUB reader during the migration period.
            return null;
        }
    }

    public static String installedEditionName(Context context) {
        File packageFile = findInstalledPackage(context);
        if (packageFile == null) return null;
        try (SqliteBibleRepository repository = SqliteBibleRepository.openReadOnly(packageFile)) {
            BibleEdition edition = repository.getEdition();
            return edition == null ? null : edition.getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static File findInstalledPackage(Context context) {
        File dir = new File(context.getFilesDir(), "bibles");
        File[] packages = dir.listFiles((ignored, name) ->
                name != null && (name.endsWith(".bible") || name.endsWith(".sqlite") || name.endsWith(".db")));
        if (packages == null || packages.length == 0) return null;
        // First phase: one preferred local edition. Package manager/version
        // selection will replace this in a later migration step.
        java.util.Arrays.sort(packages, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return packages[0].isFile() ? packages[0] : null;
    }

    private static BibleBook matchBook(List<BibleBook> books, BibleRepository.Book legacy) {
        if (books == null) return null;
        String legacyAbbr = normalize(legacy.abbreviation);
        String legacyTitle = normalize(legacy.title);
        String mappedId = canonicalId(legacy.abbreviation, legacy.title);

        for (BibleBook book : books) {
            if (book == null) continue;
            if (!TextUtils.isEmpty(mappedId) && mappedId.equalsIgnoreCase(book.getBookId())) return book;
            if (normalize(book.getShortName()).equals(legacyAbbr)) return book;
            if (normalize(book.getName()).equals(legacyTitle)) return book;
        }
        return null;
    }

    /** Minimal compatibility map for the abbreviations already used by Ministerium. */
    private static String canonicalId(String abbreviation, String title) {
        String a = normalize(abbreviation);
        String t = normalize(title);
        String probe = a + " " + t;
        if (contains(probe, "gen", "genesis")) return "GEN";
        if (contains(probe, "ex", "exodo")) return "EXO";
        if (contains(probe, "lev", "levitico")) return "LEV";
        if (contains(probe, "num", "numeros")) return "NUM";
        if (contains(probe, "dt", "deuteronomio")) return "DEU";
        if (contains(probe, "jos", "josue")) return "JOS";
        if (contains(probe, "jue", "jueces")) return "JDG";
        if (contains(probe, "rut", "ruth")) return "RUT";
        if (contains(probe, "1 sam", "1samuel")) return "1SA";
        if (contains(probe, "2 sam", "2samuel")) return "2SA";
        if (contains(probe, "1 rey", "1reyes")) return "1KI";
        if (contains(probe, "2 rey", "2reyes")) return "2KI";
        if (contains(probe, "1 cro", "1cronicas")) return "1CH";
        if (contains(probe, "2 cro", "2cronicas")) return "2CH";
        if (contains(probe, "esd", "esdras")) return "EZR";
        if (contains(probe, "neh", "nehemias")) return "NEH";
        if (contains(probe, "tob", "tobias")) return "TOB";
        if (contains(probe, "jdt", "judit")) return "JDT";
        if (contains(probe, "est", "ester")) return "EST";
        if (contains(probe, "1 mac", "1macabeos")) return "1MA";
        if (contains(probe, "2 mac", "2macabeos")) return "2MA";
        if (contains(probe, "job")) return "JOB";
        if (contains(probe, "sal", "salmos")) return "PSA";
        if (contains(probe, "pro", "proverbios")) return "PRO";
        if (contains(probe, "ecl", "eclesiastes")) return "ECC";
        if (contains(probe, "cant", "cantar")) return "SNG";
        if (contains(probe, "sab", "sabiduria")) return "WIS";
        if (contains(probe, "sir", "eclesiastico")) return "SIR";
        if (contains(probe, "is", "isaias")) return "ISA";
        if (contains(probe, "jer", "jeremias")) return "JER";
        if (contains(probe, "lam", "lamentaciones")) return "LAM";
        if (contains(probe, "bar", "baruc")) return "BAR";
        if (contains(probe, "ez", "ezequiel")) return "EZK";
        if (contains(probe, "dan", "daniel")) return "DAN";
        if (contains(probe, "os", "oseas")) return "HOS";
        if (contains(probe, "jl", "joel")) return "JOL";
        if (contains(probe, "am", "amos")) return "AMO";
        if (contains(probe, "abd", "abdias")) return "OBA";
        if (contains(probe, "jon", "jonas")) return "JON";
        if (contains(probe, "miq", "miqueas")) return "MIC";
        if (contains(probe, "nah", "nahum")) return "NAM";
        if (contains(probe, "hab", "habacuc")) return "HAB";
        if (contains(probe, "sof", "sofonias")) return "ZEP";
        if (contains(probe, "ag", "ageo")) return "HAG";
        if (contains(probe, "zac", "zacarias")) return "ZEC";
        if (contains(probe, "mal", "malaquias")) return "MAL";
        if (contains(probe, "mt", "mateo")) return "MAT";
        if (contains(probe, "mc", "marcos")) return "MRK";
        if (contains(probe, "lc", "lucas")) return "LUK";
        if (contains(probe, "jn", "juan")) return "JHN";
        if (contains(probe, "hch", "hechos")) return "ACT";
        if (contains(probe, "rom", "romanos")) return "ROM";
        if (contains(probe, "1 cor", "1corintios", "1 co")) return "1CO";
        if (contains(probe, "2 cor", "2corintios", "2 co")) return "2CO";
        if (contains(probe, "gal", "galatas")) return "GAL";
        if (contains(probe, "ef", "efesios")) return "EPH";
        if (contains(probe, "flp", "filipenses")) return "PHP";
        if (contains(probe, "col", "colosenses")) return "COL";
        if (contains(probe, "1 tes", "1tesalonicenses")) return "1TH";
        if (contains(probe, "2 tes", "2tesalonicenses")) return "2TH";
        if (contains(probe, "1 tim", "1timoteo")) return "1TI";
        if (contains(probe, "2 tim", "2timoteo")) return "2TI";
        if (contains(probe, "tit", "tito")) return "TIT";
        if (contains(probe, "flm", "filemon")) return "PHM";
        if (contains(probe, "heb", "hebreos")) return "HEB";
        if (contains(probe, "sant", "santiago")) return "JAS";
        if (contains(probe, "1 pe", "1pedro")) return "1PE";
        if (contains(probe, "2 pe", "2pedro")) return "2PE";
        if (contains(probe, "1 jn", "1juan")) return "1JN";
        if (contains(probe, "2 jn", "2juan")) return "2JN";
        if (contains(probe, "3 jn", "3juan")) return "3JN";
        if (contains(probe, "jud", "judas")) return "JUD";
        if (contains(probe, "ap", "apocalipsis")) return "REV";
        return "";
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) {
            String n = normalize(needle);
            if (value.equals(n) || value.startsWith(n + " ") || value.endsWith(" " + n)
                    || value.contains(" " + n + " ")) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String render(BibleEdition edition, BibleBook book, int chapter,
                                 List<BibleVerse> verses) {
        StringBuilder out = new StringBuilder(4096);
        out.append("<!doctype html><html><head><meta charset=\"utf-8\"></head><body>");
        out.append("<main id=\"ministerium-chapter\" data-edition=\"")
                .append(escape(edition == null ? "" : edition.getEditionId()))
                .append("\" data-book=\"").append(escape(book.getBookId())).append("\">");
        out.append("<h3 class=\"ministerium-chapter-title\">CAPÍTULO ")
                .append(chapter).append("</h3>");
        for (BibleVerse verse : verses) {
            if (verse.isHeading()) {
                out.append("<h4>").append(escape(verse.getText())).append("</h4>");
                continue;
            }
            if (verse.isParagraphStart()) out.append("<p>");
            else out.append("<p>");
            String legacyId = "v" + chapter + verse.getVerseLabel();
            out.append("<sup id=\"").append(escape(legacyId)).append("\" data-verse-id=\"")
                    .append(escape(verse.stableId())).append("\">")
                    .append(escape(verse.getVerseLabel())).append("</sup> ")
                    .append(escape(verse.getText())).append("</p>");
        }
        out.append("</main></body></html>");
        return out.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
