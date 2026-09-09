package com.fabri.ministerium

/**
 * Composes one native Hours document from already-parsed liturgical sources.
 * The merger is deliberately conservative: it never guesses a common and it
 * never borrows a block from a neighbouring saint.
 */
object HoursV5Composition {
    @JvmStatic
    fun compose(
        hourKey: String?,
        rank: String?,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?
    ): HoursNativeDocument {
        val key = hourKey.orEmpty()
        val normalizedRank = rank.orEmpty()
        if (proper == null || proper.blocks.isEmpty()) return temporal ?: empty(key)
        if (temporal == null || temporal.blocks.isEmpty()) return proper

        // Feasts and solemnities are isolated offices. If their proper package is
        // incomplete we prefer showing that incompleteness over silently importing
        // unrelated temporal material.
        if (normalizedRank == "F" || normalizedRank == "S") return proper

        // Memories keep the psalmody of the feria unless the verified proper itself
        // explicitly supplies a complete psalmody. Other proper elements may replace
        // their temporal counterparts one semantic role at a time.
        if (normalizedRank == "M" || normalizedRank == "m" || normalizedRank == "m*") {
            return composeMemory(key, temporal, proper)
        }

        // Unknown proper rank: keep it isolated rather than guessing precedence.
        return proper
    }

    private fun composeMemory(
        hourKey: String,
        temporal: HoursNativeDocument,
        proper: HoursNativeDocument
    ): HoursNativeDocument {
        val properHasPsalmody = proper.blocks.any { it.type == HoursBlockType.PSALMODY }
        val replaceable = setOf(
            HoursBlockType.HYMN,
            HoursBlockType.READING,
            HoursBlockType.RESPONSORY,
            HoursBlockType.CANTICLE,
            HoursBlockType.INTERCESSIONS,
            HoursBlockType.PRAYER
        )
        val properByType = proper.blocks.groupBy { it.type }
        val result = mutableListOf<HoursBlock>()
        val used = mutableSetOf<HoursBlockType>()

        temporal.blocks.forEach { block ->
            when {
                block.type == HoursBlockType.PSALMODY && properHasPsalmody -> {
                    if (used.add(HoursBlockType.PSALMODY)) {
                        result += properByType[HoursBlockType.PSALMODY].orEmpty()
                    }
                }
                block.type in replaceable && properByType[block.type].orEmpty().isNotEmpty() -> {
                    if (used.add(block.type)) result += properByType[block.type].orEmpty()
                }
                else -> result += block
            }
        }

        // Proper blocks not represented in the temporal skeleton are appended only
        // for roles that are safe and explicit. Generic TEXT/HEADING blocks are not
        // appended because their provenance is ambiguous outside their source.
        replaceable.forEach { type ->
            if (type !in used && temporal.blocks.none { it.type == type }) {
                result += properByType[type].orEmpty()
            }
        }

        return HoursNativeDocument(
            proper.title.ifBlank { temporal.title.ifBlank { hourKey } },
            result
        )
    }

    private fun empty(hourKey: String) = HoursNativeDocument(
        hourKey.ifBlank { "Liturgia de las Horas" },
        emptyList()
    )
}
