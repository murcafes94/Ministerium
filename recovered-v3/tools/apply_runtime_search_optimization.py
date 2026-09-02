#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'Expected block not found in {path}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


search = ROOT / 'app/src/main/java/com/fabri/ministerium/SearchActivity.java'
old_search = '''                List<SearchResult> found = SCOPE_MAGISTERIUM.equals(scope)
                        ? MagisteriumIndexRepository.search(
                                getApplicationContext(), query, 150)
                        : ContentRepository.search(
                                getApplicationContext(), query, documentId, 150);
                runOnUiThread(() -> display(found));'''
new_search = '''                String cacheKey = SearchResultCache.key(scope, documentId, query);
                List<SearchResult> found = SearchResultCache.get(cacheKey);
                if (found == null) {
                    found = SCOPE_MAGISTERIUM.equals(scope)
                            ? MagisteriumIndexRepository.search(
                                    getApplicationContext(), query, 150)
                            : ContentRepository.search(
                                    getApplicationContext(), query, documentId, 150);
                    SearchResultCache.put(cacheKey, found);
                }
                List<SearchResult> displayResults = found;
                runOnUiThread(() -> display(displayResults));'''
replace_once(search, old_search, new_search)

content = ROOT / 'app/src/main/java/com/fabri/ministerium/ContentRepository.java'
old_cache = '''    private static final Map<String, String[]> PAGE_CACHE = new ConcurrentHashMap<>();'''
new_cache = '''    private static final Map<String, String[]> PAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, String[]>(4, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > 2;
                }
            });'''
replace_once(content, old_cache, new_cache)

print('Runtime search optimization applied.')
