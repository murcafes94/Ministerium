from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Anchor not found: {path}: {old[:80]!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


# Global search: canonical law returns canon text only; commentary stays attached to canon.
patch(
    'app/src/main/java/com/fabri/ministerium/ContentRepository.java',
    '''            try {\n                results.addAll(MagisteriumIndexRepository.search(\n                        context, query, maximumResults - results.size()));\n            } catch (Exception ignored) {\n                // Una falla aislada del índice magisterial no debe impedir la búsqueda global.\n            }\n            if (results.size() >= maximumResults) return results;''',
    '''            try {\n                results.addAll(CanonSearchRepository.search(\n                        context, query, maximumResults - results.size()));\n            } catch (Exception ignored) {\n                // Derecho Canónico: solo texto de los cánones; los comentarios se abren desde el canon.\n            }\n            if (results.size() >= maximumResults) return results;\n            try {\n                results.addAll(MagisteriumIndexRepository.search(\n                        context, query, maximumResults - results.size()));\n            } catch (Exception ignored) {\n                // Una falla aislada del índice magisterial no debe impedir la búsqueda global.\n            }\n            if (results.size() >= maximumResults) return results;'''
)

patch(
    'app/src/main/java/com/fabri/ministerium/SearchActivity.java',
    '''    private void openResult(SearchResult result) {\n        if (result.isPrayer()) {''',
    '''    private void openResult(SearchResult result) {\n        if (result.isCanon()) {\n            startActivity(new Intent(this, CanonLawActivity.class)\n                    .putExtra(CanonLawActivity.EXTRA_CANON, result.canonNumber));\n            return;\n        }\n        if (result.isPrayer()) {'''
)
patch(
    'app/src/main/java/com/fabri/ministerium/SearchActivity.java',
    '''            if (result.isPrayer()) {\n                source = "Oraciones básicas";''',
    '''            if (result.isCanon()) {\n                source = "Código de Derecho Canónico · comentario disponible desde el canon";\n            } else if (result.isPrayer()) {\n                source = "Oraciones básicas";'''
)

# Santoral month cache: one TOC traversal per month per app session.
epub = 'app/src/main/java/com/fabri/ministerium/EpubUtils.java'
patch(
    epub,
    '''    private static final Map<String, List<EpubTocEntry>> TOC_CACHE = new ConcurrentHashMap<>();''',
    '''    private static final Map<String, List<EpubTocEntry>> TOC_CACHE = new ConcurrentHashMap<>();\n    private static final Map<Integer, Map<Integer, List<HoursLink>>> SAINTS_BY_MONTH_CACHE =\n            new ConcurrentHashMap<>();'''
)
patch(
    epub,
    '''    public static List<HoursLink> saintsForDate(Context context, int month, int day)\n            throws Exception {\n        HoursVolume santoral = HoursRepository.find("sanctoral");\n        List<EpubTocEntry> entries = tableOfContents(context, santoral);\n        List<HoursLink> result = new ArrayList<>();\n        String currentMonth = "";\n        String wantedMonth = MONTHS[Math.max(0, Math.min(11, month))];\n        String dayPrefix = String.valueOf(day);\n        for (int i = 0; i < entries.size(); i++) {\n            EpubTocEntry entry = entries.get(i);\n            String title = entry.title.trim();\n            if (entry.depth == 0 && isMonth(title)) currentMonth = title.toUpperCase(Locale.ROOT);\n            if (!wantedMonth.equals(currentMonth) || entry.depth == 0) continue;\n            String normalized = title.replaceAll("\\\\s+", " ");\n            if (normalized.matches("^0?" + dayPrefix + "\\\\s*-.*")) {\n                String cleanTitle = normalized.replaceFirst("^0?" + dayPrefix + "\\\\s*-\\\\s*", "");\n                result.add(new HoursLink(santoral, i, cleanTitle,\n                        "Propio del Santoral · " + wantedMonth));\n            }\n        }\n        return result;\n    }''',
    '''    public static List<HoursLink> saintsForDate(Context context, int month, int day)\n            throws Exception {\n        int safeMonth = Math.max(0, Math.min(11, month));\n        Map<Integer, List<HoursLink>> cachedMonth = SAINTS_BY_MONTH_CACHE.get(safeMonth);\n        if (cachedMonth == null) {\n            cachedMonth = buildSaintMonth(context, safeMonth);\n            SAINTS_BY_MONTH_CACHE.put(safeMonth, cachedMonth);\n        }\n        List<HoursLink> found = cachedMonth.get(day);\n        return found == null ? Collections.emptyList() : found;\n    }\n\n    private static Map<Integer, List<HoursLink>> buildSaintMonth(Context context, int month)\n            throws Exception {\n        HoursVolume santoral = HoursRepository.find("sanctoral");\n        List<EpubTocEntry> entries = tableOfContents(context, santoral);\n        Map<Integer, List<HoursLink>> result = new ConcurrentHashMap<>();\n        String currentMonth = "";\n        String wantedMonth = MONTHS[month];\n        for (int i = 0; i < entries.size(); i++) {\n            EpubTocEntry entry = entries.get(i);\n            String title = entry.title.trim();\n            if (entry.depth == 0 && isMonth(title)) currentMonth = title.toUpperCase(Locale.ROOT);\n            if (!wantedMonth.equals(currentMonth) || entry.depth == 0) continue;\n            String normalized = title.replaceAll("\\\\s+", " ");\n            java.util.regex.Matcher match = java.util.regex.Pattern\n                    .compile("^(\\\\d{1,2})\\\\s*-\\\\s*(.+)$").matcher(normalized);\n            if (!match.matches()) continue;\n            int day = Integer.parseInt(match.group(1));\n            result.computeIfAbsent(day, ignored -> new ArrayList<>()).add(\n                    new HoursLink(santoral, i, match.group(2),\n                            "Propio del Santoral · " + wantedMonth));\n        }\n        for (Map.Entry<Integer, List<HoursLink>> value : result.entrySet()) {\n            value.setValue(Collections.unmodifiableList(value.getValue()));\n        }\n        return Collections.unmodifiableMap(result);\n    }'''
)

# Centralized WebView teardown in the main readers.
for file in [
    'CanonLawActivity.java', 'HoursReaderActivity.java', 'LatinHoursReaderActivity.java',
    'MassReadingReaderActivity.java', 'ComplineReaderActivity.java',
    'CombinedHoursActivity.java', 'CombinedMassActivity.java'
]:
    path = 'app/src/main/java/com/fabri/ministerium/' + file
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    old = 'if (webView != null) webView.destroy();'
    if old in text:
        p.write_text(text.replace(old, 'if (webView != null) { WebViewCleanup.destroy(webView); webView = null; }'), encoding='utf-8')

# Bible and Missal already use the full teardown sequence; keep them as-is.
print('Ministerium performance phase 2 applied.')
