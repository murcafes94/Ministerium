package com.fabri.ministerium;

import java.io.File;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ajustes finales del documento continuo antes de mostrarlo al usuario. */
public final class CombinedMassPolisher {
    private static final Pattern LINK = Pattern.compile(
            "(?is)<a\\s+[^>]*href=[\\\"'][^\\\"']*[\\\"'][^>]*>(.*?)</a>");
    private static final Pattern CANTICLE_LINK = Pattern.compile(
            "(?is)<p\\b[^>]*>\\s*<a\\b[^>]*>\\s*(?:Benedictus|Magnificat)\\s*</a>\\s*</p>");
    private static final Pattern ROW = Pattern.compile("(?is)<tr\\b[^>]*>.*?</tr>");
    private static final Pattern MASS_SECTION = Pattern.compile(
            "(?is)(<section\\s+class=\\\"ministerium-section\\\"[^>]*>\\s*"
                    + "<h2>\\s*Santa Misa\\s*</h2>)(.*?)(</section>)");

    private CombinedMassPolisher() {}

    public static CombinedMassComposer.Result compose(android.content.Context context,
                                                       Calendar date,
                                                       String hourKey,
                                                       String language) throws Exception {
        CombinedMassComposer.Result base = CombinedMassComposer.compose(
                context, date, hourKey, language);
        String html = cleanMissalNavigation(base.html);

        LiturgicalDay day = LiturgicalResolver.resolve(context, date);
        html = applyGloriaRule(context, date, day, html);
        if (!hasRequiredSaint(day)) {
            HourEntry hour = findHour(context, day, date, hourKey);
            int ordinaryWeek = LiturgicalResolver.ordinaryWeekNumber(date);
            if (hour != null && hour.volume != null && "ordinary".equals(hour.volume.id)
                    && ordinaryWeek > 0) {
                File root = EpubUtils.ensureExtracted(context, hour.volume);
                String resolved = OrdinaryReferenceResolver.resolve(root, hour.filePath,
                        ordinaryWeek, LiturgicalResolver.lectionaryCycle(date),
                        date.get(Calendar.YEAR) % 2 == 0 ? 2 : 1);
                String psalmody = section(resolved, "SALMODIA", "LECTURA BREVE");
                String canticle = section(resolved, "CÁNTICO EVANGÉLICO", "PRECES");
                canticle = addCanticle(canticle, hourKey);
                html = replaceSection(html, "Salmodia de " + hourName(hourKey),
                        cleanLinks(psalmody));
                html = replaceSection(html, "Cántico evangélico de " + hourName(hourKey),
                        cleanLinks(canticle));
            }
        }

        return new CombinedMassComposer.Result(base.title, base.celebration, html);
    }

    private static boolean hasRequiredSaint(LiturgicalDay day) {
        if (day == null || day.saintOffices == null) return false;
        for (HoursLink office : day.saintOffices) {
            if (office != null && office.requiresProperOffice()) return true;
        }
        return false;
    }

    private static String applyGloriaRule(android.content.Context context, Calendar date,
                                          LiturgicalDay day, String html) {
        if (gloriaRequired(context, date, day)) return html;
        Matcher section = MASS_SECTION.matcher(html);
        if (!section.find()) return html;

        String content = section.group(2);
        Matcher rows = ROW.matcher(content);
        int gloriaStart = -1;
        int lastRowEnd = -1;
        while (rows.find()) {
            String text = normalize(rows.group().replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ").replace("&#160;", " "));
            if (gloriaStart < 0 && (text.equals("gloria")
                    || text.startsWith("gloria a dios")
                    || text.contains("gloria a dios en el cielo"))) {
                gloriaStart = rows.start();
            }
            if (gloriaStart >= 0) lastRowEnd = rows.end();
        }
        if (gloriaStart < 0 || lastRowEnd < gloriaStart) return html;

        String filtered = content.substring(0, gloriaStart) + content.substring(lastRowEnd);
        String replacement = section.group(1) + filtered + section.group(3);
        return section.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private static boolean gloriaRequired(android.content.Context context, Calendar date,
                                           LiturgicalDay day) {
        if (day != null && day.saintOffices != null) {
            for (HoursLink office : day.saintOffices) {
                if (office != null && office.isFeastOrSolemnity()) return true;
            }
        }
        try {
            for (LiturgicalEvent event : LiturgicalCalendarRepository.eventsFor(context, date)) {
                if (event.isFeast() || event.isSolemnity()) return true;
            }
        } catch (Exception ignored) {}

        if (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return false;
        String season = day == null || day.temporalOffice == null
                || day.temporalOffice.volume == null ? "" : day.temporalOffice.volume.id;
        return !"advent".equals(season) && !"lent".equals(season);
    }

    private static HourEntry findHour(android.content.Context context, LiturgicalDay day,
                                      Calendar date, String hourKey) throws Exception {
        List<HourEntry> hours = DailyHoursRepository.hoursFor(context, day.temporalOffice, date);
        for (HourEntry entry : hours) if (hourKey.equals(entry.key)) return entry;
        return null;
    }

    private static String section(String html, String startMarker, String endMarker) {
        int start = markerPosition(html, startMarker, 0);
        if (start < 0) return "";
        int end = markerPosition(html, endMarker, start + 1);
        if (end < 0) end = html.length();
        return html.substring(start, end);
    }

    private static int markerPosition(String html, String marker, int from) {
        if (html == null || marker == null) return -1;
        Pattern block = Pattern.compile("(?is)<(p|h1|h2|h3|h4|h5|h6)\\b[^>]*>.*?</\\1>");
        Matcher matcher = block.matcher(html);
        if (from > 0) matcher.region(Math.min(from, html.length()), html.length());
        String wanted = normalize(marker);
        while (matcher.find()) {
            String plain = matcher.group().replaceAll("<[^>]+>", " ")
                    .replace("&nbsp;", " ").replace("&#160;", " ");
            String actual = normalize(plain);
            if (actual.equals(wanted) || actual.startsWith(wanted)) return matcher.start();
        }
        return -1;
    }

    private static String replaceSection(String document, String heading, String replacement) {
        if (replacement == null || replacement.trim().isEmpty()) return document;
        Pattern section = Pattern.compile("(?is)(<section\\s+class=\\\"ministerium-section\\\"[^>]*>"
                + "\\s*<h2>\\s*" + Pattern.quote(heading) + "\\s*</h2>).*?</section>");
        Matcher matcher = section.matcher(document);
        if (!matcher.find()) return document;
        String value = matcher.group(1) + replacement + "</section>";
        return matcher.replaceFirst(Matcher.quoteReplacement(value));
    }

    private static String cleanMissalNavigation(String html) {
        String value = html;
        value = value.replaceAll("(?is)<p\\b[^>]*>\\s*Se dice\\s*[\\\"“”']?Gloria[\\\"“”']?\\.?\\s*</p>", "");
        value = value.replaceAll("(?is)<p\\b[^>]*>\\s*Misa\\s*(?:<b>.*?</b>)?\\s*(?:Prefacio)?\\s*</p>", "");
        value = value.replaceAll("(?is)<h3\\b[^>]*>\\s*Oración colecta\\s*</h3>", "");
        value = value.replaceAll("(?is)<h3\\b[^>]*>\\s*Oración sobre las ofrendas\\s*</h3>", "");
        value = value.replaceAll("(?is)<h3\\b[^>]*>\\s*Oración después de la comunión\\s*</h3>", "");
        return value;
    }

    private static String cleanLinks(String html) {
        if (html == null) return "";
        return LINK.matcher(html).replaceAll("$1");
    }

    /**
     * Sustituye el enlace que ocupa el lugar del cántico en la fuente por su texto
     * completo. De esta forma se conserva el orden Antífona → Cántico → Antífona,
     * en vez de desplazar el cántico al final de la sección.
     */
    private static String addCanticle(String html, String hourKey) {
        String value = html == null ? "" : html;
        String canticle = "vespers".equals(hourKey) ? magnificat() : benedictus();
        Matcher marker = CANTICLE_LINK.matcher(value);
        if (marker.find()) {
            return marker.replaceFirst(Matcher.quoteReplacement(canticle));
        }
        // Fallback para fuentes que no traigan el enlace esperado.
        return value + canticle;
    }

    private static String benedictus() {
        return "<div class=\"ministerium-canticle\"><h3>Benedictus · Lc 1,68-79</h3>"
                + "<p>Bendito sea el Señor, Dios de Israel, porque ha visitado y redimido a su pueblo,<br>"
                + "suscitándonos una fuerza de salvación en la casa de David, su siervo,<br>"
                + "según lo había predicho desde antiguo por boca de sus santos profetas.</p>"
                + "<p>Es la salvación que nos libra de nuestros enemigos y de la mano de todos los que nos odian;<br>"
                + "realizando la misericordia que tuvo con nuestros padres, recordando su santa alianza<br>"
                + "y el juramento que juró a nuestro padre Abrahán.</p>"
                + "<p>Para concedernos que, libres de temor, arrancados de la mano de los enemigos,<br>"
                + "le sirvamos con santidad y justicia, en su presencia, todos nuestros días.</p>"
                + "<p>Y a ti, niño, te llamarán profeta del Altísimo, porque irás delante del Señor a preparar sus caminos,<br>"
                + "anunciando a su pueblo la salvación, el perdón de sus pecados.</p>"
                + "<p>Por la entrañable misericordia de nuestro Dios, nos visitará el sol que nace de lo alto,<br>"
                + "para iluminar a los que viven en tiniebla y en sombra de muerte,<br>"
                + "para guiar nuestros pasos por el camino de la paz.</p>" + doxology() + "</div>";
    }

    private static String magnificat() {
        return "<div class=\"ministerium-canticle\"><h3>Magníficat · Lc 1,46-55</h3>"
                + "<p>Proclama mi alma la grandeza del Señor, se alegra mi espíritu en Dios, mi salvador;<br>"
                + "porque ha mirado la humillación de su esclava.</p>"
                + "<p>Desde ahora me felicitarán todas las generaciones, porque el Poderoso ha hecho obras grandes por mí: su nombre es santo,<br>"
                + "y su misericordia llega a sus fieles de generación en generación.</p>"
                + "<p>Él hace proezas con su brazo: dispersa a los soberbios de corazón,<br>"
                + "derriba del trono a los poderosos y enaltece a los humildes,<br>"
                + "a los hambrientos los colma de bienes y a los ricos los despide vacíos.</p>"
                + "<p>Auxilia a Israel, su siervo, acordándose de la misericordia<br>"
                + "—como lo había prometido a nuestros padres— en favor de Abrahán y su descendencia por siempre.</p>"
                + doxology() + "</div>";
    }

    private static String doxology() {
        return "<p>Gloria al Padre, y al Hijo, y al Espíritu Santo.<br>"
                + "Como era en el principio, ahora y siempre, por los siglos de los siglos. Amén.</p>";
    }

    private static String hourName(String hourKey) {
        return "vespers".equals(hourKey) ? "Vísperas" : "Laudes";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}
