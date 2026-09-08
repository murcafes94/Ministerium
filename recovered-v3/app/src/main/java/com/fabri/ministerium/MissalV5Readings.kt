package com.fabri.ministerium

import android.content.Context
import android.text.Html
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Transitional structured adapter for the native Ministerium 5 lectionary.
 *
 * It never downloads content and never invents readings. It only consumes the
 * already-validated local file produced by MassReadingsRepository and exposes
 * its sections as typed Kotlin data so the V5 reader does not depend on a
 * WebView. The source can be replaced later without changing the native UI.
 */
data class MissalV5Reading(
    val id: String,
    val title: String,
    val reference: String,
    val summary: String,
    val body: String
) {
    fun displayText(): String {
        val parts = listOf(summary.trim(), reference.trim(), body.trim()).filter { it.isNotEmpty() }
        return parts.joinToString("\n\n")
    }
}

data class MissalV5WordContent(
    val readings: List<MissalV5Reading>,
    val sourceLabel: String
) {
    fun reading(id: String): MissalV5Reading? = readings.firstOrNull { it.id == id }
}

object MissalV5Readings {
    private val sectionPattern = Pattern.compile(
        "<section\\s+class=\\\"([^\\\"]*)\\\"[^>]*>\\s*<h2>(.*?)</h2>(.*?)</section>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )
    private val referencePattern = Pattern.compile(
        "<p\\s+class=\\\"reading-reference\\\"[^>]*>(.*?)</p>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )
    private val summaryPattern = Pattern.compile(
        "<p\\s+class=\\\"reading-summary\\\"[^>]*>(.*?)</p>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    @JvmStatic
    fun has(context: Context, date: Calendar): Boolean = MassReadingsRepository.has(context, date)

    @JvmStatic
    fun load(context: Context, date: Calendar): MissalV5WordContent? {
        if (!has(context, date)) return null
        val html = try {
            MassReadingsRepository.read(context, date)
        } catch (_: Exception) {
            return null
        }

        val result = mutableListOf<MissalV5Reading>()
        val matcher = sectionPattern.matcher(html)
        while (matcher.find()) {
            val classes = matcher.group(1) ?: ""
            val title = plain(matcher.group(2) ?: "")
            val inner = matcher.group(3) ?: ""
            val id = idFor(title, classes)
            if (id.isEmpty()) continue

            val reference = first(referencePattern, inner)
            val summary = first(summaryPattern, inner)
            val bodyHtml = inner
                .replace(referencePattern.toRegex(), "")
                .replace(summaryPattern.toRegex(), "")
            val body = plain(bodyHtml)
            result += MissalV5Reading(id, title, reference, summary, body)
        }

        if (result.none { it.id == "first_reading" } || result.none { it.id == "gospel" }) return null
        return MissalV5WordContent(
            readings = result,
            sourceLabel = "Arquidiócesis de Guadalajara · copia local verificada"
        )
    }

    private fun first(pattern: Pattern, value: String): String {
        val matcher = pattern.matcher(value)
        return if (matcher.find()) plain(matcher.group(1) ?: "") else ""
    }

    private fun idFor(title: String, classes: String): String {
        val value = normalize(title)
        return when {
            value.contains("primera lectura") -> "first_reading"
            value.contains("salmo responsorial") || classes.contains("psalm-section", true) -> "psalm"
            value.contains("segunda lectura") -> "second_reading"
            value.contains("aclamacion") && value.contains("evangelio") -> "acclamation"
            value == "evangelio" || value.endsWith(" evangelio") -> "gospel"
            else -> ""
        }
    }

    @Suppress("DEPRECATION")
    private fun plain(html: String): String = Html.fromHtml(html).toString()
        .replace('\u00a0', ' ')
        .replace("\r", "")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n[ \\t]+"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun normalize(value: String): String = java.text.Normalizer
        .normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
}
