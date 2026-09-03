package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Final safety pass for Lauds/Vespers of saints.
 *
 * OGLH 235 c requires the concluding prayer of a memory to be the prayer of
 * the saint. The semantic composer already prefers it; this guard covers EPUB
 * forms in which the target Hour does not contain an "Oración" placeholder to
 * replace and therefore could otherwise leave the ferial/common prayer visible.
 */
public final class HoursProperPrayerFix41 {
    private static final Pattern PRAYER_HEADING = Pattern.compile(
            "<p\\b[^>]*>\\s*(?:<span\\b[^>]*>)?\\s*Oraci[oó]n\\s*(?:</span>)?\\s*</p>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PARAGRAPH = Pattern.compile(
            "<p\\b[^>]*>.*?</p>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private HoursProperPrayerFix41() {}

    public static void inject(WebView webView) {
        if (webView == null) return;
        Context context = webView.getContext();
        if (!(context instanceof Activity)) return;
        // En el lector bilingüe la oración propia española jamás se inyecta
        // en el panel latino.
        if (context instanceof BilingualHoursReaderActivity
                && webView.getId() != R.id.spanishWebView) return;

        Intent intent = ((Activity) context).getIntent();
        String hour = value(intent.getStringExtra(HoursReaderActivity.EXTRA_MEMORY_HOUR_KEY));
        if (hour.isEmpty()) {
            hour = value(intent.getStringExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_HOUR));
        }
        if (!("lauds".equals(hour) || "vespers".equals(hour))) return;

        String volumeId = value(intent.getStringExtra(
                HoursReaderActivity.EXTRA_MEMORY_SAINT_VOLUME_ID));
        if (volumeId.isEmpty()) {
            volumeId = value(intent.getStringExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_VOLUME));
        }
        int tocIndex = intent.getIntExtra(HoursReaderActivity.EXTRA_MEMORY_SAINT_TOC_INDEX, -1);
        if (tocIndex < 0) {
            tocIndex = intent.getIntExtra(BilingualHoursReaderActivity.EXTRA_MEMORY_INDEX, -1);
        }
        if (volumeId.isEmpty() || tocIndex < 0) return;

        try {
            String prayer = properPrayer(context, volumeId, tocIndex);
            if (prayer.isEmpty()) return;
            String script = "(function(){if(document.body.getAttribute('data-ministerium-proper-prayer')==='1')return;"
                    + "document.body.setAttribute('data-ministerium-proper-prayer','1');"
                    + "function n(v){return(v||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').replace(/\\s+/g,' ').trim().toUpperCase();}"
                    + "var nodes=document.querySelectorAll('p,h1,h2,h3,h4'),h=null;for(var i=0;i<nodes.length;i++){if(n(nodes[i].textContent)==='ORACION')h=nodes[i];}"
                    + "var box=document.createElement('div');box.innerHTML=" + JSONObject.quote(prayer) + ";"
                    + "if(h&&h.parentNode){var parent=h.parentNode,next=h.nextElementSibling;if(next&&next.tagName==='P')next.remove();"
                    + "while(box.firstChild)parent.insertBefore(box.firstChild,h);h.remove();return;}"
                    + "var conclusion=null;for(var j=0;j<nodes.length;j++){if(n(nodes[j].textContent)==='CONCLUSION'){conclusion=nodes[j];break;}}"
                    + "var target=conclusion&&conclusion.parentNode?conclusion:document.body.firstChild;var owner=conclusion&&conclusion.parentNode?conclusion.parentNode:document.body;"
                    + "while(box.firstChild)owner.insertBefore(box.firstChild,target||null);"
                    + "})()";
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {
            // Nunca se bloquea la Hora por un fallo de esta salvaguarda editorial.
        }
    }

    private static String properPrayer(Context context, String volumeId, int tocIndex)
            throws Exception {
        HoursVolume volume = HoursRepository.find(volumeId);
        if (volume == null) return "";
        List<EpubTocEntry> toc = EpubUtils.tableOfContents(context, volume);
        if (tocIndex < 0 || tocIndex >= toc.size()) return "";
        EpubTocEntry entry = toc.get(tocIndex);
        File root = EpubUtils.ensureExtracted(context, volume);
        String html = read(new File(root, entry.filePath));
        String section = section(html, entry.fragment);

        Matcher heading = PRAYER_HEADING.matcher(section);
        int start = -1;
        int end = -1;
        while (heading.find()) {
            start = heading.start();
            end = heading.end();
        }
        if (start < 0) return "";
        Matcher paragraph = PARAGRAPH.matcher(section);
        while (paragraph.find(end)) {
            String plain = paragraph.group().replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
            if (!plain.isEmpty()) {
                return section.substring(start, end) + paragraph.group();
            }
            end = paragraph.end();
        }
        return "";
    }

    private static String section(String html, String fragment) {
        if (html == null || html.isEmpty()) return "";
        if (fragment == null || fragment.isEmpty()) return body(html);
        int marker = idPosition(html, fragment);
        if (marker < 0) return body(html);
        int start = tagStart(html, marker);
        int next = html.indexOf("id=\"sigil_toc_id_", marker + fragment.length());
        if (next < 0) next = html.indexOf("id='sigil_toc_id_", marker + fragment.length());
        int end = next < 0 ? bodyEnd(html) : tagStart(html, next);
        return html.substring(start, Math.max(start, end));
    }

    private static int idPosition(String html, String fragment) {
        int result = html.indexOf("id=\"" + fragment + "\"");
        if (result < 0) result = html.indexOf("id='" + fragment + "'");
        return result;
    }

    private static int tagStart(String html, int from) {
        int result = html.lastIndexOf('<', Math.max(0, from));
        return result < 0 ? Math.max(0, from) : result;
    }

    private static String body(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<body");
        if (start < 0) return html;
        start = html.indexOf('>', start);
        if (start < 0) return html;
        int end = bodyEnd(html);
        return html.substring(start + 1, Math.max(start + 1, end));
    }

    private static int bodyEnd(String html) {
        int end = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
        return end < 0 ? html.length() : end;
    }

    private static String read(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
