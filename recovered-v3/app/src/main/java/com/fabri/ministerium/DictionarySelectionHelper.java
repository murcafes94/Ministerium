package com.fabri.ministerium;

import android.app.Activity;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.List;

public final class DictionarySelectionHelper {
    private DictionarySelectionHelper() {}

    public static void attach(Activity activity, WebView webView) {
        // Compatibilidad con lectores 2.x. En 3.0 seleccionar texto no ejecuta
        // nada: UniversalSelectionMenu añade la acción explícita «Diccionario».
    }

    public static void showDictionary(Activity activity, String word) {
        lookup(activity, word, true);
    }

    public static void showTranslator(Activity activity, String text) {
        lookup(activity, text, false);
    }

    private static void lookup(Activity activity, String word, boolean includeDictionary) {
        word = normalize(word);
        if (word.length() < 2 || word.length() > 600
                || !word.matches("(?s).*\\p{L}.*")) return;
        String preview = word.length() > 42 ? word.substring(0, 42) + "…" : word;
        Toast.makeText(activity, includeDictionary ? "Consultando «" + preview + "»…"
                : "Preparando traducción…",
                Toast.LENGTH_SHORT).show();
        final String selected = word;
        new Thread(() -> {
            try {
                StringBuilder html = new StringBuilder();
                boolean oneWord = selected.matches("[\\p{L}ÁÉÍÓÚÜÑáéíóúüñ]{2,60}");
                if (includeDictionary && oneWord) {
                    List<BibleDictionaryRepository.QuickResult> results =
                            BibleDictionaryRepository.quickLookup(activity, selected);
                    for (BibleDictionaryRepository.QuickResult result : results) {
                        html.append(result.html);
                    }
                }
                if (!includeDictionary || html.length() == 0) {
                    html.append(includeDictionary && oneWord
                            ? "<p>No se encontró una entrada local exacta.</p>" : "");
                }
                if (!includeDictionary) html.append(translationCard(selected));
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) return;
                    ReaderOverlayDialog.show(activity,
                            (includeDictionary ? "Diccionario · " : "Traducir · ")
                                    + preview, html.toString());
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        "No se pudo consultar los diccionarios.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static String normalize(String value) {
        return (value == null ? "" : value).trim().replaceAll("\\s+", " ")
                .replaceAll("^[^\\p{L}\\p{N}ÁÉÍÓÚÜÑáéíóúüñ]+|"
                        + "[^\\p{L}\\p{N}ÁÉÍÓÚÜÑáéíóúüñ.!?,;:¿?¡!—–-]+$", "");
    }

    private static String decode(String json) {
        if (json == null || "null".equals(json)) return "";
        try {
            return new org.json.JSONTokener(json).nextValue().toString().trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("^[^\\p{L}]+|[^\\p{L}.!,;:¿?¡!—–-]+$", "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String translationCard(String text) {
        String encoded = android.net.Uri.encode(text);
        return "<article class=\"dictionary-card translation-card\">"
                + "<h2>Traducir en línea</h2>"
                + "<p class=\"dictionary-source\">Detección automática del idioma · requiere Internet</p>"
                + "<p>Elige el idioma de destino. El texto se abrirá dentro de este mismo recuadro.</p>"
                + "<p class=\"translation-actions\">"
                + "<a class=\"translation-button\" href=\"ministerium://translate?target=es&amp;text="
                + encoded + "\">Español</a> "
                + "<a class=\"translation-button\" href=\"ministerium://translate?target=la&amp;text="
                + encoded + "\">Latín</a> "
                + "<a class=\"translation-button\" href=\"ministerium://translate?target=en&amp;text="
                + encoded + "\">Inglés</a></p>"
                + "<p class=\"dictionary-source\">Al tocar un idioma, el texto seleccionado se envía a Google Translate.</p>"
                + "</article>";
    }
}
