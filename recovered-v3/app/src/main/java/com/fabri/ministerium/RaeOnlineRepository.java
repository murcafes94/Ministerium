package com.fabri.ministerium;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;

/** Optional online DLE lookup through rae-api.com. Offline dictionaries remain primary. */
public final class RaeOnlineRepository {
    private static final String ENDPOINT = "https://rae-api.com/api/words/";
    private static final long MAX_CACHE_AGE = 45L * 24L * 60L * 60L * 1000L;

    private RaeOnlineRepository() {}

    public static String actionCard(String word) {
        String safe = escape(word);
        return "<article class=\"dictionary-card\"><h2>RAE · consulta en línea</h2>"
                + "<p class=\"dictionary-source\">Complemento opcional. Los diccionarios locales funcionan sin conexión.</p>"
                + "<p><a class=\"translation-button\" href=\"ministerium://rae?word="
                + url(word) + "\">Consultar «" + safe + "» en RAE</a></p>"
                + "<p class=\"dictionary-source\">Servicio no oficial de rae-api.com; el contenido lexicográfico pertenece a la RAE.</p></article>";
    }

    public static String lookupHtml(Context context, String word) throws Exception {
        String normalized = normalizeWord(word);
        if (normalized.isEmpty() || normalized.contains(" ")) {
            throw new IllegalArgumentException("Selecciona una sola palabra.");
        }
        String json = cached(context, normalized);
        boolean fromCache = json != null;
        if (json == null) {
            json = download(normalized);
            save(context, normalized, json);
        }
        return render(normalized, new JSONObject(json), fromCache);
    }

    private static String download(String word) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                ENDPOINT + URLEncoder.encode(word, "UTF-8").replace("+", "%20")).openConnection();
        connection.setConnectTimeout(4500);
        connection.setReadTimeout(6500);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Ministerium/3.1.1 Android");
        try {
            int status = connection.getResponseCode();
            if (status == 404) throw new IllegalStateException("La RAE no devolvió una entrada para esta palabra.");
            if (status == 429) throw new IllegalStateException("Se alcanzó temporalmente el límite de consultas RAE. Inténtalo más tarde.");
            if (status < 200 || status >= 300) throw new IllegalStateException(
                    "La consulta RAE no está disponible ahora (HTTP " + status + ").");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    if (output.size() > 2_000_000) throw new IllegalStateException("Respuesta RAE demasiado grande.");
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String render(String word, JSONObject root, boolean cached) {
        JSONObject data = root.optJSONObject("data");
        if (data == null) data = root;
        String displayWord = data.optString("word", word);
        JSONArray meanings = data.optJSONArray("meanings");
        if (meanings == null || meanings.length() == 0) {
            throw new IllegalStateException("La respuesta RAE no contiene definiciones utilizables.");
        }
        StringBuilder html = new StringBuilder("<article class=\"dictionary-card\"><h2>")
                .append(escape(displayWord)).append("</h2><p class=\"dictionary-source\">RAE · ")
                .append(cached ? "caché local" : "consulta en línea")
                .append(" · vía rae-api.com</p>");
        int shown = 0;
        for (int i = 0; i < meanings.length() && shown < 4; i++) {
            JSONObject meaning = meanings.optJSONObject(i);
            if (meaning == null) continue;
            JSONObject origin = meaning.optJSONObject("origin");
            if (origin != null) {
                String raw = origin.optString("raw");
                if (!raw.isEmpty()) html.append("<p><em>").append(escape(raw)).append("</em></p>");
            }
            JSONArray senses = meaning.optJSONArray("senses");
            if (senses == null) continue;
            for (int j = 0; j < senses.length() && shown < 8; j++) {
                JSONObject sense = senses.optJSONObject(j);
                if (sense == null) continue;
                String description = sense.optString("description");
                if (description.isEmpty()) description = sense.optString("raw");
                if (description.isEmpty()) continue;
                shown++;
                html.append("<p><strong>").append(shown).append(".</strong> ")
                        .append(escape(description)).append("</p>");
                appendWords(html, "Sinónimos", sense.optJSONArray("synonyms"));
                appendWords(html, "Antónimos", sense.optJSONArray("antonyms"));
                JSONArray examples = sense.optJSONArray("examples");
                if (examples != null && examples.length() > 0) {
                    html.append("<p class=\"dictionary-source\">Ej.: ")
                            .append(escape(stringValue(examples.opt(0)))).append("</p>");
                }
            }
        }
        if (shown == 0) throw new IllegalStateException("No se encontraron acepciones para esta palabra.");
        return html.append("<p class=\"dictionary-source\">Consulta complementaria; no sustituye los diccionarios offline de Ministerium.</p></article>").toString();
    }

    private static void appendWords(StringBuilder html, String label, JSONArray array) {
        if (array == null || array.length() == 0) return;
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < array.length() && i < 12; i++) {
            String value = stringValue(array.opt(i));
            if (value.isEmpty()) continue;
            if (words.length() > 0) words.append(" · ");
            words.append(escape(value));
        }
        if (words.length() > 0) html.append("<p><strong>").append(label).append(":</strong> ")
                .append(words).append("</p>");
    }

    private static String stringValue(Object value) {
        if (value instanceof String) return (String) value;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String word = object.optString("word");
            if (!word.isEmpty()) return word;
            return object.optString("text");
        }
        return value == null ? "" : value.toString();
    }

    private static String cached(Context context, String word) {
        try {
            File file = cacheFile(context, word);
            if (!file.isFile() || System.currentTimeMillis() - file.lastModified() > MAX_CACHE_AGE) return null;
            try (FileInputStream input = new FileInputStream(file);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void save(Context context, String word, String json) {
        try {
            File file = cacheFile(context, word);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    private static File cacheFile(Context context, String word) throws Exception {
        String hash = hex(MessageDigest.getInstance("SHA-256").digest(word.getBytes(StandardCharsets.UTF_8)));
        return new File(new File(context.getFilesDir(), "dictionary_cache/rae"), hash + ".json");
    }

    private static String normalizeWord(String word) {
        String value = word == null ? "" : word.replaceAll("^[^\\p{L}]+|[^\\p{L}]+$", "").trim();
        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    private static String url(String value) {
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (Exception ignored) { return ""; }
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte b : bytes) value.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return value.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
