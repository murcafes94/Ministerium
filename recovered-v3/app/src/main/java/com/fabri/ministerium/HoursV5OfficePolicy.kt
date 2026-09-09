package com.fabri.ministerium

/**
 * Ministerium 5 selection policy for the Liturgy of the Hours.
 *
 * It chooses one liturgical source at a time. It never merges a temporal office
 * with a saint or common implicitly, which prevents content from a neighbouring
 * celebration leaking into the selected day.
 */
enum class HoursOfficeSource {
    TEMPORAL,
    PROPER
}

data class HoursOfficeOption(
    val source: HoursOfficeSource,
    val office: HoursLink,
    val title: String,
    val subtitle: String,
    val automatic: Boolean
)

data class HoursOfficeSelection(
    val defaultOption: HoursOfficeOption?,
    val options: List<HoursOfficeOption>,
    val reason: String
)

object HoursV5OfficePolicy {
    @JvmStatic
    fun resolve(day: LiturgicalDay?): HoursOfficeSelection {
        if (day == null) return HoursOfficeSelection(null, emptyList(), "Día litúrgico no resuelto.")

        val options = mutableListOf<HoursOfficeOption>()
        day.temporalOffice?.let { temporal ->
            options += HoursOfficeOption(
                source = HoursOfficeSource.TEMPORAL,
                office = temporal,
                title = day.celebration.ifBlank { "Oficio del día" },
                subtitle = "Temporal · ${day.psalterWeek.ifBlank { "salterio del día" }}",
                automatic = false
            )
        }

        day.saintOffices.forEach { saint ->
            options += HoursOfficeOption(
                source = HoursOfficeSource.PROPER,
                office = saint,
                title = saint.title,
                subtitle = when {
                    saint.isFeastOrSolemnity() -> "Propio · fiesta o solemnidad"
                    saint.isMandatoryMemorial() -> "Propio · memoria obligatoria"
                    saint.isOptionalMemorial() -> "Propio · memoria libre"
                    else -> "Propio del santoral"
                },
                automatic = saint.requiresProperOffice()
            )
        }

        val required = options.firstOrNull {
            it.source == HoursOfficeSource.PROPER && it.automatic
        }
        val temporal = options.firstOrNull { it.source == HoursOfficeSource.TEMPORAL }
        val chosen = required ?: temporal ?: options.firstOrNull()

        val reason = when {
            required != null -> "La celebración propia tiene precedencia. Se carga como una fuente aislada, sin mezclar el temporal."
            day.saintOffices.any { it.isOptionalMemorial() } -> "Hay memoria libre: se conserva el oficio temporal hasta que el usuario elija expresamente el propio."
            chosen?.source == HoursOfficeSource.TEMPORAL -> "Se usa el oficio temporal del día."
            chosen != null -> "Se usa el propio disponible como fuente aislada."
            else -> "No hay un oficio verificable para esta fecha."
        }

        return HoursOfficeSelection(chosen, options.toList(), reason)
    }
}
