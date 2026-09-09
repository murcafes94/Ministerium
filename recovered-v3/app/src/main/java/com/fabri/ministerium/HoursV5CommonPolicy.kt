package com.fabri.ministerium

import java.text.Normalizer
import java.util.Locale

/**
 * Conservative validation for common-office choices found in the selected
 * santoral entry. It only removes choices that are grammatically incompatible
 * with an unambiguous Spanish saint title; it never invents a positive common.
 */
object HoursV5CommonPolicy {
    @JvmStatic
    fun filter(saint: HoursLink?, choices: List<CommonOfficeChoice>?): List<CommonOfficeChoice> {
        if (saint == null || choices.isNullOrEmpty()) return emptyList()
        val saintTitle = normalize(saint.title)
        val clearlyMale = saintTitle.startsWith("SAN ") && !saintTitle.startsWith("SANTA ")
        val clearlyFemale = saintTitle.startsWith("SANTA ") || saintTitle.startsWith("BEATA ")

        val seen = linkedSetOf<String>()
        return choices.filter { choice ->
            val common = normalize(choice.title)
            val incompatible = when {
                clearlyMale && (common.contains("VIRGENES") || common.contains("SANTAS MUJERES")) -> true
                clearlyFemale && common.contains("SANTOS VARONES") -> true
                else -> false
            }
            !incompatible && seen.add(common)
        }
    }

    private fun normalize(value: String?): String = Normalizer.normalize(
        value.orEmpty(), Normalizer.Form.NFD
    )
        .replace(Regex("\\p{M}+"), "")
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), " ")
        .trim()
}
