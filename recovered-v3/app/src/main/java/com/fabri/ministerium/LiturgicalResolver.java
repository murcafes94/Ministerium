package com.fabri.ministerium;

import android.content.Context;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class LiturgicalResolver {
    private static final String[] WEEKDAYS = {
            "", "DOMINGO", "LUNES", "MARTES", "MIÉRCOLES",
            "JUEVES", "VIERNES", "SÁBADO"
    };

    private LiturgicalResolver() {}

    public static LiturgicalDay resolve(Context context, Calendar selected) throws Exception {
        Calendar date = normalized(selected);
        List<LiturgicalEvent> events = LiturgicalCalendarRepository.eventsFor(context, date);
        LiturgicalEvent primary = primaryEvent(events);
        String celebration = primary == null ? "Feria del día" : primary.summary;
        String psalter = "";
        for (LiturgicalEvent event : events) {
            if (!event.psalterWeek.isEmpty()) {
                psalter = event.psalterWeek;
                break;
            }
        }
        String color = primary == null ? "" : primary.color;

        HoursLink temporal = temporalOffice(context, date, celebration, psalter);
        List<HoursLink> saints = matchingSaints(
                context, date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), events);
        String source = !events.isEmpty()
                ? "Calendario litúrgico de Ecuador " + date.get(Calendar.YEAR) + " · GCatholic"
                : "Cálculo del calendario romano general";
        if (color.isEmpty()) color = defaultColor(temporal);
        return new LiturgicalDay(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH),
                LiturgicalCalendarRepository.dateLabel(date), celebration, source,
                psalter, color, temporal, saints);
    }

    /**
     * Una memoria libre nunca sustituye por defecto a la feria. Se conserva en
     * saintOffices para que el usuario pueda elegirla explícitamente.
     */
    public static LiturgicalEvent primaryEvent(List<LiturgicalEvent> events) {
        if (events == null || events.isEmpty()) return null;
        for (LiturgicalEvent event : events) {
            if (event != null && !event.isOptionalMemorial()) return event;
        }
        return null;
    }

    public static int ordinaryWeekNumber(Calendar selected) {
        Calendar date = normalized(selected);
        int year = date.get(Calendar.YEAR);
        Calendar easter = easterSunday(year);
        Calendar ashWednesday = addDays(easter, -46);
        Calendar pentecost = addDays(easter, 49);
        Calendar advent = adventStart(year);
        Calendar baptism = baptismOfLord(year);

        if (date.after(baptism) && date.before(ashWednesday)) {
            Calendar firstMonday = addDays(baptism, 1);
            int week = daysBetween(firstMonday, date) / 7 + 1;
            if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) week++;
            return Math.max(1, week);
        }
        if (date.after(pentecost) && date.before(advent)) {
            Calendar christKing = addDays(advent, -7);
            int remaining = daysBetween(date, christKing);
            return 34 - ((Math.max(0, remaining) + 6) / 7);
        }
        return 0;
    }

    public static String lectionaryCycle(Calendar selected) {
        Calendar date = normalized(selected);
        int liturgicalYear = date.get(Calendar.YEAR);
        if (!date.before(adventStart(liturgicalYear))) liturgicalYear++;
        int cycle = ((liturgicalYear % 3) + 3) % 3;
        if (cycle == 1) return "A";
        if (cycle == 2) return "B";
        return "C";
    }

    private static String defaultColor(HoursLink temporal) {
        if (temporal == null || temporal.volume == null) return "";
        if ("ordinary".equals(temporal.volume.id)) return "Verde";
        if ("advent".equals(temporal.volume.id) || "lent".equals(temporal.volume.id)) {
            return "Morado";
        }
        return "Blanco";
    }

    private static HoursLink temporalOffice(Context context, Calendar date,
                                            String celebration, String psalter)
            throws Exception {
        String normalizedCelebration = normalize(celebration);
        HoursVolume volume;
        String target;

        if (normalizedCelebration.contains("epifania del senor")) {
            volume = HoursRepository.find("christmas");
            target = "EPIFANÍA DEL SEÑOR";
        } else if (normalizedCelebration.contains("bautismo del senor")) {
            volume = HoursRepository.find("christmas");
            target = "EL BAUTISMO DEL SEÑOR";
        } else if (normalizedCelebration.contains("santa maria madre de dios")) {
            volume = HoursRepository.find("christmas");
            target = "SANTA MARÍA, MADRE DE DIOS";
        } else if (normalizedCelebration.contains("sagrada familia")) {
            volume = HoursRepository.find("christmas");
            target = "Sagrada Familia";
        } else if (normalizedCelebration.contains("domingo de pascua de la resurreccion")) {
            volume = HoursRepository.find("easter");
            target = "DOMINGO DE PASCUA DE LA RESURRECCIÓN DEL SEÑOR";
        } else if (normalizedCelebration.contains("ascension del senor")) {
            volume = HoursRepository.find("easter");
            target = "LA ASCENSIÓN DEL SEÑOR";
        } else if (normalizedCelebration.contains("pentecostes")) {
            volume = HoursRepository.find("easter");
            target = "PENTECOSTÉS";
        } else if (normalizedCelebration.contains("domingo de ramos")) {
            volume = HoursRepository.find("lent");
            target = "DOMINGO DE RAMOS";
        } else if (normalizedCelebration.contains("cristo rey")
                || normalizedCelebration.contains("rey del universo")) {
            volume = HoursRepository.find("ordinary");
            target = "DOM. XXXIV: CRISTO REY";
        } else if (normalizedCelebration.contains("santisima trinidad")) {
            volume = HoursRepository.find("ordinary");
            target = "LA SANTÍSIMA TRINIDAD";
        } else if (normalizedCelebration.contains("cuerpo y la sangre")
                || normalizedCelebration.contains("cuerpo y sangre")) {
            volume = HoursRepository.find("ordinary");
            target = "CUERPO Y SANGRE DE CRISTO";
        } else if (normalizedCelebration.contains("sagrado corazon")) {
            volume = HoursRepository.find("ordinary");
            target = "SAGRADO CORAZÓN DE JESÚS";
        } else {
            Target calculated = calculateTarget(date, psalter);
            volume = HoursRepository.find(calculated.volumeId);
            target = calculated.title;
        }

        int index = EpubUtils.findEntryIndex(context, volume, target);
        if (index < 0) return null;
        EpubTocEntry entry = EpubUtils.tableOfContents(context, volume).get(index);
        return new HoursLink(volume, index, entry.title,
                "Oficio temporal · " + volume.title);
    }

    private static Target calculateTarget(Calendar date, String suppliedPsalter) {
        int year = date.get(Calendar.YEAR);
        Calendar easter = easterSunday(year);
        Calendar ashWednesday = addDays(easter, -46);
        Calendar palmSunday = addDays(easter, -7);
        Calendar pentecost = addDays(easter, 49);
        Calendar advent = adventStart(year);
        Calendar christmas = day(year, Calendar.DECEMBER, 25);
        Calendar baptism = baptismOfLord(year);
        int weekDay = date.get(Calendar.DAY_OF_WEEK);
        String weekday = WEEKDAYS[weekDay];

        if (!date.before(advent) && date.before(christmas)) {
            if (date.get(Calendar.MONTH) == Calendar.DECEMBER
                    && date.get(Calendar.DAY_OF_MONTH) >= 17
                    && weekDay != Calendar.SUNDAY) {
                return new Target("advent", "DÍA " + date.get(Calendar.DAY_OF_MONTH)
                        + " DE DICIEMBRE");
            }
            int week = daysBetween(advent, date) / 7 + 1;
            return new Target("advent", weekday + " " + roman(week));
        }

        if (!date.before(christmas)) {
            int day = date.get(Calendar.DAY_OF_MONTH);
            if (day == 25) return new Target("christmas", "LA NATIVIDAD DEL SEÑOR");
            if (day >= 26 && day <= 28) {
                String title = day == 26 ? "26 - San Esteban"
                        : day == 27 ? "27 - San Juan" : "28 - Santos Inocentes";
                return new Target("christmas", title);
            }
            return new Target("christmas", day + " de diciembre");
        }

        if (date.get(Calendar.MONTH) == Calendar.JANUARY && !date.after(baptism)) {
            int day = date.get(Calendar.DAY_OF_MONTH);
            if (day == 1) return new Target("christmas", "SANTA MARÍA, MADRE DE DIOS");
            return new Target("christmas", day + " de enero");
        }

        if (!date.before(ashWednesday) && date.before(easter)) {
            if (sameDay(date, ashWednesday)) return new Target("lent", "MIÉRCOLES DE CENIZA");
            Calendar firstSunday = addDays(ashWednesday, 4);
            if (date.before(firstSunday)) {
                return new Target("lent", weekday + " DESPUÉS DE CENIZA");
            }
            if (!date.before(palmSunday)) {
                if (weekDay == Calendar.SUNDAY) return new Target("lent", "DOMINGO DE RAMOS");
                return new Target("lent", weekday + " SANTO");
            }
            int week = daysBetween(firstSunday, date) / 7 + 1;
            String title = weekDay == Calendar.SUNDAY
                    ? "DOMINGO " + roman(week) + " DE CUARESMA"
                    : weekday + " " + roman(week) + " DE CUARESMA";
            return new Target("lent", title);
        }

        if (!date.before(easter) && !date.after(pentecost)) {
            int offset = daysBetween(easter, date);
            if (offset < 7) {
                if (offset == 0) return new Target("easter",
                        "DOMINGO DE PASCUA DE LA RESURRECCIÓN DEL SEÑOR");
                return new Target("easter", weekday + " DE LA OCTAVA DE PASCUA");
            }
            int week = offset / 7 + 1;
            String title = weekDay == Calendar.SUNDAY
                    ? "DOMINGO " + roman(week) + " DE PASCUA"
                    : weekday + " " + roman(week) + " DE PASCUA";
            return new Target("easter", title);
        }

        int psalter = romanValue(suppliedPsalter);
        if (psalter < 1 || psalter > 4) {
            int ordinaryWeek;
            if (date.before(ashWednesday)) {
                ordinaryWeek = daysBetween(addDays(baptism, 1), date) / 7 + 1;
            } else {
                Calendar christKing = addDays(advent, -7);
                int remaining = daysBetween(date, christKing);
                ordinaryWeek = 34 - ((Math.max(0, remaining) + 6) / 7);
            }
            psalter = ((ordinaryWeek - 1) % 4 + 4) % 4 + 1;
        }
        return new Target("ordinary", weekday + " " + roman(psalter));
    }

    private static List<HoursLink> matchingSaints(Context context, int month, int day,
                                                   List<LiturgicalEvent> events)
            throws Exception {
        List<HoursLink> available = EpubUtils.saintsForDate(context, month, day);
        if (events.isEmpty()) return available;
        List<HoursLink> result = new ArrayList<>();
        for (HoursLink link : available) {
            for (LiturgicalEvent event : events) {
                if (sameCelebration(link.title, event.summary)) {
                    result.add(new HoursLink(link.volume, link.tocIndex, link.title,
                            event.isOptionalMemorial()
                                    ? "Memoria libre · puede elegirse para el oficio"
                                    : event.rankLabel(),
                            event.color, event.rank));
                    break;
                }
            }
        }
        return result;
    }

    private static boolean sameCelebration(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        if (a.contains(b) || b.contains(a)) return true;
        for (String token : a.split(" ")) {
            if (token.length() >= 5 && !isCommon(token) && b.contains(token)) return true;
        }
        return false;
    }

    private static boolean isCommon(String value) {
        return "santo".equals(value) || "santa".equals(value) || "santos".equals(value)
                || "virgen".equals(value) || "martir".equals(value)
                || "presbitero".equals(value) || "obispo".equals(value)
                || "iglesia".equals(value) || "senor".equals(value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static int romanValue(String value) {
        if ("I".equals(value)) return 1;
        if ("II".equals(value)) return 2;
        if ("III".equals(value)) return 3;
        if ("IV".equals(value)) return 4;
        return 0;
    }

    private static String roman(int value) {
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII"};
        return value >= 1 && value < romans.length ? romans[value] : String.valueOf(value);
    }

    private static Calendar easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return day(year, month - 1, day);
    }

    private static Calendar adventStart(int year) {
        Calendar date = day(year, Calendar.DECEMBER, 3);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) date.add(Calendar.DATE, -1);
        return date;
    }

    private static Calendar epiphanySunday(int year) {
        Calendar date = day(year, Calendar.JANUARY, 2);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) date.add(Calendar.DATE, 1);
        return date;
    }

    /**
     * En los lugares donde Epifanía se traslada al domingo 2–8 de enero, si
     * cae el 7 u 8 el Bautismo del Señor se celebra el lunes siguiente; en los
     * demás casos, el domingo siguiente. Esta fecha también determina cuándo
     * comienza el primer tramo del Tiempo Ordinario.
     */
    private static Calendar baptismOfLord(int year) {
        Calendar epiphany = epiphanySunday(year);
        int day = epiphany.get(Calendar.DAY_OF_MONTH);
        return addDays(epiphany, day >= 7 ? 1 : 7);
    }

    private static Calendar day(int year, int month, int day) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month, day, 12, 0, 0);
        return date;
    }

    private static Calendar normalized(Calendar source) {
        return day(source.get(Calendar.YEAR), source.get(Calendar.MONTH),
                source.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar addDays(Calendar source, int amount) {
        Calendar result = (Calendar) source.clone();
        result.add(Calendar.DATE, amount);
        return result;
    }

    private static int daysBetween(Calendar start, Calendar end) {
        long difference = normalized(end).getTimeInMillis() - normalized(start).getTimeInMillis();
        return (int) Math.round(difference / 86400000d);
    }

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static final class Target {
        final String volumeId;
        final String title;

        Target(String volumeId, String title) {
            this.volumeId = volumeId;
            this.title = title;
        }
    }
}
