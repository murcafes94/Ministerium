package com.fabri.ministerium

import android.text.Html
import java.util.Locale

enum class HoursBlockType {
    HEADING,
    HYMN,
    ANTIPHON,
    GOSPEL_ANTIPHON,
    PSALMODY,
    READING,
    FIRST_READING,
    SECOND_READING,
    RESPONSORY,
    CANTICLE,
    INTERCESSIONS,
    PRAYER,
    TEXT
}

data class HoursBlock(
    val type: HoursBlockType,
    val title: String,
    val body: String
)

data class HoursNativeDocument(
    val title: String,
    val blocks: List<HoursBlock>
)

object HoursV5DocumentParser {
    @JvmStatic
    fun parse(title: String, html: String, fragment: String?, scrollText: String?): HoursNativeDocument {
        val scoped = scopeFragment(html, fragment)
        val prepared = scoped
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "</p>\n")
            .replace(Regex("(?i)</div>"), "</div>\n")
            .replace(Regex("(?i)</h[1-6]>"), "$0\n")
            .replace(Regex("(?i)</li>"), "</li>\n")
        val plain = Html.fromHtml(prepared).toString()
            .replace('\u00a0', ' ')
            .replace("\r", "")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        val narrowed = narrowByScrollText(plain, scrollText)
        if (narrowed.isBlank()) return HoursNativeDocument(title, emptyList())

        val blocks = mutableListOf<HoursBlock>()
        val paragraphs = narrowed.split(Regex("\\n\\s*\\n|(?m)^(?=[A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ 0-9.,;:()/-]{3,}$)"))
            .map { clean(it) }
            .filter { it.isNotBlank() }

        paragraphs.forEach { paragraph ->
            val firstLine = paragraph.lineSequence().firstOrNull()?.trim().orEmpty()
            val type = classify(title, firstLine, paragraph)
            val titlePart = when (type) {
                HoursBlockType.HYMN -> "Himno"
                HoursBlockType.ANTIPHON -> "Antífona"
                HoursBlockType.GOSPEL_ANTIPHON -> gospelAntiphonTitle(title)
                HoursBlockType.PSALMODY -> psalmTitle(firstLine)
                HoursBlockType.READING -> readingTitle(firstLine)
                HoursBlockType.FIRST_READING -> readingTitle(firstLine).ifBlank { "Primera lectura" }
                HoursBlockType.SECOND_READING -> readingTitle(firstLine).ifBlank { "Segunda lectura" }
                HoursBlockType.RESPONSORY -> "Responsorio"
                HoursBlockType.CANTICLE -> canticleTitle(firstLine)
                HoursBlockType.INTERCESSIONS -> "Preces"
                HoursBlockType.PRAYER -> prayerTitle(firstLine)
                HoursBlockType.HEADING -> firstLine
                HoursBlockType.TEXT -> ""
            }
            val body = if (type == HoursBlockType.GOSPEL_ANTIPHON) {
                stripHourPrefix(paragraph)
            } else if (titlePart.isNotEmpty() && normalize(firstLine).contains(normalize(titlePart))) {
                paragraph.substring(firstLine.length).trim()
            } else paragraph
            if (body.isNotBlank() || titlePart.isNotBlank()) blocks += HoursBlock(type, titlePart, body)
        }
        return HoursNativeDocument(title, blocks)
    }

    private fun scopeFragment(html: String, fragment: String?): String {
        val id = fragment?.trim().orEmpty()
        if (id.isEmpty()) return html
        val escaped = Regex.escape(id)
        val start = Regex("(?is)<[^>]+(?:id|name)=[\\\"']$escaped[\\\"'][^>]*>").find(html)?.range?.first ?: return html
        val tail = html.substring(start)
        val next = Regex("(?is)<(?:h1|h2|section|article)[^>]+(?:id|name)=[\\\"'][^\\\"']+[\\\"'][^>]*>")
            .find(tail, 1)?.range?.first
        return if (next == null) tail else tail.substring(0, next)
    }

    private fun narrowByScrollText(text: String, scrollText: String?): String {
        val wanted = scrollText?.trim().orEmpty()
        if (wanted.isEmpty()) return text
        val index = normalize(text).indexOf(normalize(wanted))
        if (index < 0) return text
        val direct = text.lowercase(Locale.ROOT).indexOf(wanted.lowercase(Locale.ROOT))
        return if (direct >= 0) text.substring(direct) else text
    }

    private fun classify(documentTitle: String, firstLine: String, paragraph: String): HoursBlockType {
        val n = normalize(firstLine)
        val document = normalize(documentTitle)
        return when {
            isGospelAntiphon(document, n, paragraph) -> HoursBlockType.GOSPEL_ANTIPHON
            n == "HIMNO" || n.startsWith("HIMNO ") -> HoursBlockType.HYMN
            n.startsWith("ANT ") || n.startsWith("ANTIFONA") -> HoursBlockType.ANTIPHON
            n.startsWith("SALMO") || n.startsWith("CANTICO AT") || n.startsWith("CANTICO NT") -> HoursBlockType.PSALMODY
            n.startsWith("PRIMERA LECTURA") -> HoursBlockType.FIRST_READING
            n.startsWith("SEGUNDA LECTURA") -> HoursBlockType.SECOND_READING
            n.startsWith("LECTURA") -> HoursBlockType.READING
            n.startsWith("RESPONSORIO") || n.startsWith("RESP BREVE") -> HoursBlockType.RESPONSORY
            n.contains("BENEDICTUS") || n.contains("MAGNIFICAT") || n.contains("NUNC DIMITTIS") -> HoursBlockType.CANTICLE
            n == "PRECES" || n.startsWith("PRECES ") -> HoursBlockType.INTERCESSIONS
            n.startsWith("ORACION") || n == "PADRE NUESTRO" || n.startsWith("ORACION CONCLUSIVA") -> HoursBlockType.PRAYER
            looksLikeHeading(firstLine, paragraph) -> HoursBlockType.HEADING
            else -> HoursBlockType.TEXT
        }
    }

    private fun isGospelAntiphon(document: String, first: String, paragraph: String): Boolean {
        if (paragraph.length > 650) return false
        val lauds = document.contains("LAUDES") && first.startsWith("LAUDES ")
        val vespers = document.contains("VISPERAS") && first.startsWith("VISPERAS ")
        val explicit = first.startsWith("ANTIFONA DEL CANTICO EVANGELICO")
        return lauds || vespers || explicit
    }

    private fun stripHourPrefix(value: String): String = value
        .replaceFirst(Regex("(?i)^\\s*(Laudes|V[ií]speras)\\s*:\\s*"), "")
        .replaceFirst(Regex("(?i)^\\s*Ant[ií]fona del c[aá]ntico evang[eé]lico\\s*:?\\s*"), "")
        .trim()

    private fun gospelAntiphonTitle(documentTitle: String): String {
        val n = normalize(documentTitle)
        return if (n.contains("LAUDES")) "Antífona del Benedictus"
        else if (n.contains("VISPERAS")) "Antífona del Magníficat"
        else "Antífona del cántico evangélico"
    }

    private fun looksLikeHeading(firstLine: String, paragraph: String): Boolean {
        if (paragraph.contains('\n')) return false
        if (firstLine.length !in 4..80) return false
        val letters = firstLine.filter { it.isLetter() }
        return letters.isNotEmpty() && letters.count { it.isUpperCase() } >= letters.length * 0.75
    }

    private fun psalmTitle(line: String) = if (line.length <= 80) line else "Salmodia"
    private fun readingTitle(line: String) = if (line.length <= 80) line else "Lectura"
    private fun canticleTitle(line: String): String {
        val n = normalize(line)
        return when {
            n.contains("BENEDICTUS") -> "Cántico evangélico · Benedictus"
            n.contains("MAGNIFICAT") -> "Cántico evangélico · Magníficat"
            n.contains("NUNC DIMITTIS") -> "Cántico evangélico · Nunc dimittis"
            else -> "Cántico evangélico"
        }
    }
    private fun prayerTitle(line: String) = if (line.length <= 80) line else "Oración"

    private fun clean(value: String) = value.trim().replace(Regex("[ \\t]+"), " ")
    private fun normalize(value: String) = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), " ")
        .trim()
}
