package com.fabri.ministerium

import android.content.Context
import java.util.Calendar
import java.util.Locale

/**
 * Conservative presentation rules for optional elements in the native Missal.
 * These rules never manufacture liturgical content: they only decide whether
 * an already-defined semantic element should be shown for the selected day.
 */
data class MissalDayPresentation(
    val showGloria: Boolean,
    val showCreed: Boolean,
    val showSecondReading: Boolean,
    val allowEucharisticPrayerIV: Boolean,
    val reason: String
)

object MissalDisplayRules {
    /** Calendar-backed resolver used by the V5 native reader. */
    @JvmStatic
    fun resolve(context: Context, date: Calendar, celebration: String?): MissalDayPresentation {
        return try {
            val normalized = (celebration ?: "").lowercase(Locale.ROOT)
            val day = LiturgicalResolver.resolve(context, date)
            val primary = LiturgicalResolver.primaryEvent(
                LiturgicalCalendarRepository.eventsFor(context, date)
            )
            val sunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val solemnity = primary?.isSolemnity == true || normalized.contains("solemnidad")
            val feast = primary?.isFeast == true || normalized.contains("fiesta")
            val season = day.temporalOffice?.volume?.id.orEmpty()
            val advent = season == "advent"
            val lent = season == "lent"
            val ordinary = season == "ordinary" || LiturgicalResolver.ordinaryWeekNumber(date) > 0

            val gloria = solemnity || feast || (sunday && !advent && !lent)
            val creed = solemnity || sunday
            val secondReading = solemnity || sunday

            // GIRM 365d: Prayer IV has an invariable preface and is used when
            // the Mass has no proper preface; it is suitable on Sundays in
            // Ordinary Time. Be conservative outside Ordinary Time and on
            // feasts/solemnities where a proper preface is expected.
            val allowPrayerIV = ordinary && !solemnity && !feast

            val reason = when {
                solemnity -> "Solemnidad: Gloria, Credo y segunda lectura activos. La Plegaria IV queda restringida por su prefacio invariable."
                feast -> "Fiesta: Gloria activo; Credo y segunda lectura dependen del formulario. La Plegaria IV queda restringida por su prefacio invariable."
                sunday && advent -> "Domingo de Adviento: sin Gloria; Credo y segunda lectura activos."
                sunday && lent -> "Domingo de Cuaresma: sin Gloria; Credo y segunda lectura activos."
                sunday && ordinary -> "Domingo del Tiempo Ordinario: Gloria, Credo y segunda lectura activos."
                sunday -> "Domingo: Credo y segunda lectura activos; el Gloria depende del tiempo litúrgico."
                else -> "Feria o memoria: se muestran solo los elementos requeridos por el día."
            }

            MissalDayPresentation(gloria, creed, secondReading, allowPrayerIV, reason)
        } catch (_: Exception) {
            resolve(date, celebration)
        }
    }

    /** Fallback without Context, kept for compatibility with older callers. */
    @JvmStatic
    fun resolve(date: Calendar, celebration: String?): MissalDayPresentation {
        val normalized = (celebration ?: "").lowercase(Locale.ROOT)
        val sunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val solemnity = normalized.contains("solemnidad")
        val feast = normalized.contains("fiesta")
        val advent = normalized.contains("adviento")
        val lent = normalized.contains("cuaresma")
        val ordinary = normalized.contains("tiempo ordinario") || LiturgicalResolver.ordinaryWeekNumber(date) > 0

        val gloria = solemnity || feast || (sunday && !advent && !lent)
        val creed = solemnity || sunday
        val secondReading = solemnity || sunday
        val allowPrayerIV = ordinary && !solemnity && !feast

        val reason = when {
            solemnity -> "Solemnidad: se activan los elementos dominicales principales."
            feast -> "Fiesta: Gloria activo; Credo y segunda lectura dependen del formulario propio."
            sunday && advent -> "Domingo de Adviento: sin Gloria; Credo y segunda lectura activos."
            sunday && lent -> "Domingo de Cuaresma: sin Gloria; Credo y segunda lectura activos."
            sunday -> "Domingo: Gloria, Credo y segunda lectura activos."
            else -> "Feria o memoria: se muestran solo los elementos ordinariamente requeridos."
        }

        return MissalDayPresentation(gloria, creed, secondReading, allowPrayerIV, reason)
    }
}
