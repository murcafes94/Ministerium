package com.fabri.ministerium;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ranked, offline, full-text search over the Magisterium EPUB collection.
 * The TSV index is produced at build time by build_magisterium_index_40.py.
 */
public final class MagisteriumIndexRepository {
    private static final String INDEX_ASSET = "magisterium-index.tsv";

    private MagisteriumIndexRepository() {}

    public static List<SearchResult> search(Context context, String query, int maximum)
            throws Exception {
        String wanted = normalize(query);
        if (wanted.length() < 2 || maximum <= 0) return Collections.emptyList();

        String[] terms = wanted.split(" ");
        List<ScoredHit> hits = new ArrayList<>();
        int dataRows = 0;
        try (InputStream input = context.getAssets().open(INDEX_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] fields = line.split("\t", 6);
                if (fields.length < 6) continue;
                dataRows++;

                HoursVolume volume = HoursRepository.find(fields[0]);
                if (volume == null || !HoursRepository.isReference(volume)) continue;
                String haystack = fields[4];
                int matchedTerms = 0;
                for (String term : terms) {
                    if (!term.isEmpty() && haystack.contains(term)) matchedTerms++;
                }
                int required = terms.length <= 2
                        ? terms.length : Math.max(2, (int) Math.ceil(terms.length * 0.70));
                if (matchedTerms < required) continue;

                String normalizedTitle = normalize(fields[2]);
                String normalizedReference = normalize(fields[3]);
                int score = matchedTerms * 8;
                if (haystack.contains(wanted)) score += 35;
                if (normalizedTitle.contains(wanted)) score += 55;
                if (normalizedReference.contains(wanted)) score += 50;
                int first = haystack.indexOf(terms[0]);
                if (first >= 0) score += Math.max(0, 18 - Math.min(18, first / 90));

                String label = "Magisterio · " + volume.title;
                if (!fields[3].isEmpty() && !fields[3].equals(fields[2])) {
                    label += " · " + fields[3];
                }
                SearchResult result = new SearchResult(volume, fields[1], fields[2],
                        label, query.trim(), fields[5]);
                hits.add(new ScoredHit(result, score));
            }
        } catch (IOException missingIndex) {
            return fallbackTocSearch(context, query, maximum);
        }

        if (dataRows == 0) return fallbackTocSearch(context, query, maximum);
        hits.sort(new Comparator<ScoredHit>() {
            @Override public int compare(ScoredHit left, ScoredHit right) {
                int score = Integer.compare(right.score, left.score);
                if (score != 0) return score;
                return left.result.title.compareToIgnoreCase(right.result.title);
            }
        });

        List<SearchResult> results = new ArrayList<>();
        Map<String, Integer> perEntry = new HashMap<>();
        for (ScoredHit hit : hits) {
            String key = hit.result.hoursVolume.id + "|" + hit.result.directFilePath;
            int count = perEntry.containsKey(key) ? perEntry.get(key) : 0;
            if (count >= 3) continue;
            perEntry.put(key, count + 1);
            results.add(hit.result);
            if (results.size() >= maximum) break;
        }
        return results;
    }

    private static List<SearchResult> fallbackTocSearch(
            Context context, String query, int maximum) throws Exception {
        String wanted = normalize(query);
        List<SearchResult> results = new ArrayList<>();
        for (HoursVolume volume : HoursRepository.references()) {
            List<EpubTocEntry> entries = EpubUtils.tableOfContents(context, volume);
            String section = volume.title;
            for (int index = 0; index < entries.size(); index++) {
                EpubTocEntry entry = entries.get(index);
                if (entry.depth == 0) section = entry.title;
                String haystack = normalize(entry.title + " " + section + " " + volume.title);
                if (!haystack.contains(wanted)) continue;
                results.add(new SearchResult(volume, index, entry.title,
                        "Magisterio · " + volume.title + " · " + section));
                if (results.size() >= maximum) return results;
            }
        }
        return results;
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static final class ScoredHit {
        final SearchResult result;
        final int score;

        ScoredHit(SearchResult result, int score) {
            this.result = result;
            this.score = score;
        }
    }
}
