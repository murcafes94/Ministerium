package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Describe el ciclo y la regla de selección que deben acompañar a las lecturas.
 *
 * La fuente normativa es la Ordenación de las Lecturas de la Misa (OLM),
 * especialmente nn. 65, 69, 79, 82-89 y 103-109. Este motor no sustituye al
 * Leccionario ni inventa perícopas: valida la identidad del día y explica qué
 * serie debe haber resuelto el paquete de lecturas.
 */
public final class LectionaryRuleEngine {
    public static final String SOURCE_URL =
            "https://liturgiapapal.org/attachments/article/731/Ordenacion%20Lecturas%20Misa.pdf";

    public static final class Selection {
        public final int liturgicalYear;
        public final String sundayCycle;
        public final String weekdayCycle;
        public final String kind;
        public final String cycleLabel;
        public final String rule;
        public final String sourceReference;

        Selection(int liturgicalYear, String sundayCycle, String weekdayCycle,
                  String kind, String cycleLabel, String rule, String sourceReference) {
            this.liturgicalYear = liturgicalYear;
            this.sundayCycle = sundayCycle;
            this.weekdayCycle = weekdayCycle;
            this.kind = kind;
            this.cycleLabel = cycleLabel;
            this.rule = rule;
            this.sourceReference = sourceReference;
        }

        public String summary() {
            return kind + " · " + cycleLabel + " · " + rule;
        }
    }

    private LectionaryRuleEngine() {}

    public static Selection resolve(Context context, Calendar date) {
        List<LiturgicalEvent> events;
        try {
            events = LiturgicalCalendarRepository.eventsFor(context, date);
        } catch (Exception ignored) {
            events = Collections.emptyList();
        }
        LiturgicalEvent primary = events.isEmpty() ? null : events.get(0);
        int liturgicalYear = liturgicalYear(date);
        String sundayCycle = sundayCycle(liturgicalYear);

        // OLM 69 assigns ferial cycle I to odd civil years and II to even civil
        // years. It does not change at Advent together with the Sunday cycle.
        String weekdayCycle = date.get(Calendar.YEAR) % 2 == 0 ? "II" : "I";
        boolean ordinaryTime = isOrdinaryTime(context, date);

        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return new Selection(liturgicalYear, sundayCycle, weekdayCycle,
                    "Domingo", "ciclo dominical " + sundayCycle,
                    "tres lecturas; el salmo permanece unido a la primera lectura",
                    "OLM 65, 66, 79 y 89");
        }
        if (primary != null && primary.isSolemnity()) {
            return new Selection(liturgicalYear, sundayCycle, weekdayCycle,
                    "Solemnidad", "ciclo dominical " + sundayCycle,
                    "Propio o Común; ordinariamente tres lecturas",
                    "OLM 84");
        }
        if (primary != null && primary.isFeast()) {
            return new Selection(liturgicalYear, sundayCycle, weekdayCycle,
                    "Fiesta", "Propio o Común",
                    "lecturas asignadas a la celebración",
                    "OLM 83-84");
        }
        if (primary != null
                && (primary.isMandatoryMemorial() || primary.isOptionalMemorial())) {
            return new Selection(liturgicalYear, sundayCycle, weekdayCycle,
                    primary.isMandatoryMemorial() ? "Memoria" : "Memoria libre",
                    ordinaryTime ? "serie ferial " + weekdayCycle : "serie ferial anual",
                    "lecturas del día salvo lectura propia expresamente indicada",
                    "OLM 69 y 82-84");
        }
        return new Selection(liturgicalYear, sundayCycle, weekdayCycle,
                "Feria", ordinaryTime ? "ciclo ferial " + weekdayCycle : "ciclo anual",
                ordinaryTime
                        ? "primera lectura del año " + weekdayCycle + ", Evangelio anual y salmo asignado"
                        : "lecturas propias del tiempo y salmo asignado",
                "OLM 65, 69, 82 y 89");
    }

    private static boolean isOrdinaryTime(Context context, Calendar date) {
        try {
            LiturgicalDay day = LiturgicalResolver.resolve(context, date);
            return day.temporalOffice != null
                    && day.temporalOffice.volume != null
                    && "ordinary".equals(day.temporalOffice.volume.id);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * El año litúrgico que empieza en Adviento toma el número del año civil
     * siguiente. Ejemplo: desde Adviento de 2025 se usa el año litúrgico 2026.
     */
    static int liturgicalYear(Calendar date) {
        int civilYear = date.get(Calendar.YEAR);
        Calendar advent = firstSundayOfAdvent(civilYear);
        return date.before(advent) ? civilYear : civilYear + 1;
    }

    static String sundayCycle(int liturgicalYear) {
        int remainder = Math.floorMod(liturgicalYear, 3);
        if (remainder == 1) return "A";
        if (remainder == 2) return "B";
        return "C";
    }

    private static Calendar firstSundayOfAdvent(int year) {
        Calendar result = Calendar.getInstance();
        result.clear();
        result.set(year, Calendar.NOVEMBER, 27, 12, 0, 0);
        while (result.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            result.add(Calendar.DATE, 1);
        }
        return result;
    }
}
