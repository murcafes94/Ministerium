package com.fabri.ministerium

import android.content.Context
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** Dated upstream offices are kept separate from the local Ecuador calendar. */
object HoursDailyRepository {
    const val EXTRA_ASSET = "hours_daily_asset"
    const val EXTRA_DATE = "hours_daily_date"
    private var manifest: JSONObject? = null

    @Synchronized @JvmStatic
    fun day(context: Context, date: Calendar): JSONObject? {
        if (manifest == null) {
            manifest = try { context.assets.open("hours-daily/manifest.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) } }
            catch (_: Exception) { return null }
        }
        val key = String.format(Locale.US, "%04d-%02d-%02d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH))
        return manifest?.optJSONObject("days")?.optJSONObject(key)
    }

    @JvmStatic
    fun document(context: Context, asset: String, expectedDate: String, title: String): HoursNativeDocument {
        require(Regex("\\d{4}-\\d{2}-\\d{2}/(?:day|\\d+)/[a-z]+\\.json").matches(asset))
        val value = context.assets.open("hours-daily/$asset").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        require(value.getString("date") == expectedDate) { "La fecha del oficio no coincide." }
        val paragraphs = value.getJSONArray("paragraphs")
        val blocks = (0 until paragraphs.length()).map { index ->
            val text = paragraphs.getString(index).trim()
            val letters = text.filter { it.isLetter() }
            val heading = !text.contains('\n') && letters.isNotEmpty() &&
                (letters.count { it.isUpperCase() } > letters.length * .8 || text.startsWith("Himno:") || text.startsWith("Salmo "))
            if (heading) HoursBlock(HoursBlockType.HEADING, text, "")
            else HoursBlock(HoursBlockType.TEXT, "", text)
        }.filter { it.title.isNotBlank() || it.body.isNotBlank() }
        require(blocks.isNotEmpty()) { "Oficio vacío." }
        return HoursNativeDocument(title, blocks)
    }
}
