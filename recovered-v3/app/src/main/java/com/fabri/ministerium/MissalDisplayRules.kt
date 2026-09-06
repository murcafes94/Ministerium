package com.fabri.ministerium

import java.util.Calendar
import java.util.Locale

/**
 * Conservative presentation rules for optional elements in the native Missal.
 * These rules never manufacture liturgical content: they only decide whether
 * an already-defined semantic element should be shown for the selected day.
 * A fuller Ecuador calendar resolver can replace this object without changing
 * the reader UI.
 */
data class MissalDayPresentation(
    val showGloria: Boolean,
    val showCreed: Boolean,
    val showSecondReading: Boolean,
    val reason: String
)

object MissalDisplayRules {
    @JvmStatic
    fun resolve(date: Calendar, celebration: String?): MissalDayPresentation {
        val normalized = (celebration ?: "").lowercase(Locale.ROOT)
        val sunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val solemnity = normalized.contains("solemnidad")
        val feast = normalized.contains("fiesta")
        val advent = normalized.contains("adviento")
        val lent = normalized.contains("cuaresma")

        val gloria = solemnity || feast || (sunday && !advent && !lent)
        val creed = solemnity || sunday
        val secondReading = solemnity || sunday

        val reason = when {
            solemnity -> "Solemnidad: se activan los elementos dominicales principales."
            feast -> "Fiesta: Gloria activo; Credo y segunda lectura dependen del formulario propio."
            sunday && advent -> "Domingo de Adviento: sin Gloria; Credo y segunda lectura activos."
            sunday && lent -> "Domingo de Cuaresma: sin Gloria; Credo y segunda lectura activos."
            sunday -> "Domingo: Gloria, Credo y segunda lectura activos."
            else -> "Feria o memoria: se muestran solo los elementos ordinariamente requeridos."
        }

        return MissalDayPresentation(gloria, creed, secondReading, reason)
    }
}