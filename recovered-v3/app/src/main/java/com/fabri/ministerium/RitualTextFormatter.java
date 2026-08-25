package com.fabri.ministerium;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.text.Normalizer;
import java.util.Locale;

/** Native visual structure for ritual texts without changing their wording. */
public final class RitualTextFormatter {
    private RitualTextFormatter() {}

    public static CharSequence format(Context context, String source) {
        if (source == null) return "";
        boolean dark = ThemeUtils.isDark(context);
        int accent = Color.parseColor(dark ? "#D9B96F" : "#6E1D2A");
        int muted = Color.parseColor(dark ? "#C8BDB0" : "#766B61");
        int responseBg = Color.parseColor(dark ? "#3B332C" : "#F4E9D1");
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = source.replace("\r", "").split("\n", -1);

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
                continue;
            }
            int start = out.length();
            out.append(line).append('\n');
            int end = out.length() - 1;
            String normalized = normalize(line);

            if (isHeading(line, normalized)) {
                out.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.12f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(accent), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (isResponse(normalized)) {
                out.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(responseBg), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new LeadingMarginSpan.Standard(18, 18), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (isRubric(normalized)) {
                out.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(.90f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(muted), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (isMinisterCue(normalized)) {
                out.setSpan(new ForegroundColorSpan(accent), start,
                        Math.min(end, start + Math.min(3, line.length())), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return out;
    }

    private static boolean isHeading(String raw, String normalized) {
        if (raw.length() > 120) return false;
        boolean letters = raw.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*");
        if (!letters) return false;
        if (raw.equals(raw.toUpperCase(new Locale("es", "EC")))) return true;
        return normalized.startsWith("capitulo ") || normalized.startsWith("rito de ")
                || normalized.startsWith("bendicion de ") || normalized.startsWith("oracion de ")
                || normalized.equals("oracion") || normalized.equals("lectura de la palabra de dios")
                || normalized.equals("preces") || normalized.equals("monicion");
    }

    private static boolean isResponse(String value) {
        return value.startsWith("r. ") || value.startsWith("℟. ") || value.equals("amen")
                || value.startsWith("todos: ") || value.startsWith("todos responden")
                || value.startsWith("el pueblo responde");
    }

    private static boolean isMinisterCue(String value) {
        return value.startsWith("v. ") || value.startsWith("℣. ")
                || value.startsWith("sacerdote: ") || value.startsWith("diacono: ");
    }

    private static boolean isRubric(String value) {
        return value.startsWith("el sacerdote ") || value.startsWith("el diacono ")
                || value.startsWith("el celebrante ") || value.startsWith("a continuacion ")
                || value.startsWith("luego ") || value.startsWith("despues ")
                || value.startsWith("seguidamente ") || value.startsWith("mientras ")
                || value.startsWith("si se ") || value.startsWith("si parece ")
                || value.startsWith("cuando ") || value.startsWith("entonces ")
                || value.startsWith("todos se ") || value.startsWith("los fieles ")
                || value.startsWith("terminada ") || value.startsWith("acabado ");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }
}
