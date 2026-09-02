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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Estructura visual nativa para Ritual y Bendicional sin modificar el texto.
 * Títulos, celebrante, asamblea y rúbricas reciben jerarquías claramente distintas.
 */
public final class RitualTextFormatter {
    private RitualTextFormatter() {}

    public static CharSequence format(Context context, String source) {
        if (source == null) return "";
        source = joinBrokenProseContinuations(source);
        boolean dark = ThemeUtils.isDark(context);
        int accent = Color.parseColor(dark ? "#D9B96F" : "#6E1D2A");
        int ink = Color.parseColor(dark ? "#F3EDE4" : "#2A2521");
        int muted = Color.parseColor(dark ? "#C8BDB0" : "#766B61");
        int celebrantBg = Color.parseColor(dark ? "#332C28" : "#F8F1E5");
        int responseBg = Color.parseColor(dark ? "#43382E" : "#F2E3C4");
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = source.replace("\r", "").split("\n", -1);

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                appendBreak(out);
                continue;
            }
            String normalized = normalize(line);
            boolean assembly = isResponse(normalized);
            boolean rubric = !assembly && isRubric(normalized);
            boolean celebrant = !assembly && !rubric && isMinisterSpeech(normalized);
            boolean heading = !assembly && !rubric && !celebrant && isHeading(line, normalized);

            if (heading) ensureSectionBreak(out);
            int start = out.length();
            out.append(line).append('\n');
            int end = out.length() - 1;

            if (assembly) {
                out.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(responseBg), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(ink), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new LeadingMarginSpan.Standard(22, 22), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (rubric) {
                out.setSpan(new StyleSpan(Typeface.ITALIC), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(.90f), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(muted), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new LeadingMarginSpan.Standard(28, 28), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (celebrant) {
                out.setSpan(new BackgroundColorSpan(celebrantBg), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new LeadingMarginSpan.Standard(12, 12), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                int cueEnd = ministerCueEnd(line, start, end);
                out.setSpan(new StyleSpan(Typeface.BOLD), start, cueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(accent), start, cueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (heading) {
                out.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.15f), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(accent), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return out;
    }

    private static String joinBrokenProseContinuations(String source) {
        String[] raw = source.replace("\r", "").split("\n", -1);
        List<String> out = new ArrayList<>();
        for (String value : raw) {
            String line = value.trim();
            if (line.isEmpty()) {
                out.add("");
                continue;
            }
            if (!out.isEmpty()) {
                int last = out.size() - 1;
                String previous = out.get(last).trim();
                if (!previous.isEmpty() && shouldJoin(previous, line)) {
                    out.set(last, previous + " " + line);
                    continue;
                }
            }
            out.add(line);
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) result.append('\n');
            result.append(out.get(i));
        }
        return result.toString();
    }

    private static boolean shouldJoin(String previous, String next) {
        if (!(previous.endsWith(",") || previous.endsWith(";") || previous.endsWith(":"))) {
            return false;
        }
        if (next.isEmpty() || !Character.isLowerCase(next.codePointAt(0))) return false;
        String left = normalize(previous);
        String right = normalize(next);
        if (isResponse(left) || isRubric(left) || isMinisterSpeech(left)
                || isHeading(previous, left)) return false;
        if (isResponse(right) || isRubric(right) || isMinisterSpeech(right)
                || isHeading(next, right)) return false;
        return true;
    }

    private static void appendBreak(SpannableStringBuilder out) {
        if (out.length() == 0) return;
        if (out.charAt(out.length() - 1) != '\n') out.append('\n');
        if (out.length() < 2 || out.charAt(out.length() - 2) != '\n') out.append('\n');
    }

    private static void ensureSectionBreak(SpannableStringBuilder out) {
        if (out.length() == 0) return;
        appendBreak(out);
    }

    private static int ministerCueEnd(String line, int start, int end) {
        int colon = line.indexOf(':');
        if (colon >= 0 && colon < 24) return Math.min(end, start + colon + 1);
        int dot = line.indexOf('.');
        if (dot >= 0 && dot < 4) return Math.min(end, start + dot + 1);
        return Math.min(end, start + Math.min(12, line.length()));
    }

    private static boolean isHeading(String raw, String normalized) {
        if (raw.length() > 120) return false;
        boolean letters = raw.matches(".*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ].*");
        if (!letters) return false;
        if (raw.equals(raw.toUpperCase(new Locale("es", "EC")))) return true;
        return normalized.startsWith("capitulo ") || normalized.startsWith("rito de ")
                || normalized.startsWith("bendicion de ") || normalized.startsWith("oracion de ")
                || normalized.equals("oracion") || normalized.equals("lectura de la palabra de dios")
                || normalized.equals("liturgia de la palabra") || normalized.equals("preces")
                || normalized.equals("monicion") || normalized.equals("saludo")
                || normalized.equals("bendicion") || normalized.equals("aspersion")
                || normalized.equals("conclusion del rito");
    }

    private static boolean isResponse(String value) {
        return value.startsWith("r. ") || value.startsWith("r: ") || value.startsWith("℟. ")
                || value.equals("amen") || value.equals("amen.")
                || value.startsWith("todos: ") || value.startsWith("asamblea: ")
                || value.startsWith("pueblo: ") || value.startsWith("fieles: ")
                || value.startsWith("todos responden") || value.startsWith("responden:")
                || value.startsWith("el pueblo responde") || value.startsWith("los fieles responden")
                || value.startsWith("y con tu espiritu") || value.startsWith("demos gracias a dios")
                || value.startsWith("te alabamos, senor");
    }

    private static boolean isMinisterSpeech(String value) {
        return value.startsWith("v. ") || value.startsWith("v: ") || value.startsWith("℣. ")
                || value.startsWith("sacerdote: ") || value.startsWith("celebrante: ")
                || value.startsWith("diacono: ") || value.startsWith("ministro: ")
                || value.startsWith("presidente: ") || value.startsWith("capellan: ");
    }

    private static boolean isRubric(String value) {
        return value.startsWith("el sacerdote ") || value.startsWith("el diacono ")
                || value.startsWith("el celebrante ") || value.startsWith("el ministro ")
                || value.startsWith("el que preside ") || value.startsWith("a continuacion ")
                || value.startsWith("luego ") || value.startsWith("despues ")
                || value.startsWith("seguidamente ") || value.startsWith("mientras ")
                || value.startsWith("si se ") || value.startsWith("si parece ")
                || value.startsWith("si las circunstancias ") || value.startsWith("cuando ")
                || value.startsWith("entonces ") || value.startsWith("todos se ")
                || value.startsWith("los fieles ") || value.startsWith("la asamblea ")
                || value.startsWith("terminada ") || value.startsWith("terminado ")
                || value.startsWith("acabado ") || value.startsWith("acabada ")
                || value.startsWith("de pie ") || value.startsWith("sentados ")
                || value.startsWith("de rodillas ") || value.startsWith("se hace ")
                || value.startsWith("puede hacerse ") || value.startsWith("se puede ")
                || value.startsWith("el rito ") || value.startsWith("el celebrante puede ");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }
}
