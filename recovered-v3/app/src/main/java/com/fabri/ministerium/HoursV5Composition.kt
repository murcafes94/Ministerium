package com.fabri.ministerium

/**
 * Composes one native Hours document from already-parsed liturgical sources.
 * Every source remains explicit: temporal, proper and (only when resolved by the
 * santoral entry or chosen by the user) common. No neighbouring celebration is
 * ever used as an implicit fallback.
 */
object HoursV5Composition {
    @JvmStatic
    fun compose(
        hourKey: String?,
        rank: String?,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?,
        common: HoursNativeDocument?
    ): HoursNativeDocument {
        val key = hourKey.orEmpty()
        val normalizedRank = rank.orEmpty()
        val hasProper = proper != null && proper.blocks.isNotEmpty()
        val hasCommon = common != null && common.blocks.isNotEmpty()
        val hasTemporal = temporal != null && temporal.blocks.isNotEmpty()

        if (!hasProper && !hasCommon) return temporal ?: empty(key)

        // A feast or solemnity is never repaired from the feria. If an explicit
        // common was selected/referenced, it may form the base and the proper
        // replaces only matching semantic roles.
        if (normalizedRank == "F" || normalizedRank == "S") {
            return when {
                hasCommon && hasProper -> overlay(common!!, proper!!, key, allowPsalmody = true)
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty(key)
            }
        }

        // Memorials keep the feria as their base. Proper material has priority;
        // an explicit common is only a secondary source for roles absent from the
        // proper. Psalmody remains temporal unless the proper itself supplies a
        // complete psalmody; a common never replaces memorial psalmody silently.
        if (normalizedRank == "M" || normalizedRank == "m" || normalizedRank == "m*") {
            if (!hasTemporal) return when {
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty(key)
            }
            return composeMemory(key, temporal!!, proper, common)
        }

        // Unknown rank: keep the selected non-temporal source isolated rather
        // than guessing precedence.
        return when {
            hasProper -> proper!!
            hasCommon -> common!!
            hasTemporal -> temporal!!
            else -> empty(key)
        }
    }

    // Backward-compatible overload while callers migrate to explicit commons.
    @JvmStatic
    fun compose(
        hourKey: String?,
        rank: String?,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?
    ): HoursNativeDocument = compose(hourKey, rank, temporal, proper, null)

    private fun composeMemory(
        hourKey: String,
        temporal: HoursNativeDocument,
        proper: HoursNativeDocument?,
        common: HoursNativeDocument?
    ): HoursNativeDocument {
        val properBlocks = proper?.blocks.orEmpty()
        val commonBlocks = common?.blocks.orEmpty()
        val properHasPsalmody = properBlocks.any { it.type == HoursBlockType.PSALMODY }
        val replaceable = linkedSetOf(
            HoursBlockType.HYMN,
            HoursBlockType.READING,
            HoursBlockType.RESPONSORY,
            HoursBlockType.CANTICLE,
            HoursBlockType.INTERCESSIONS,
            HoursBlockType.PRAYER
        )
        val properByType = properBlocks.groupBy { it.type }
        val commonByType = commonBlocks.groupBy { it.type }
        val result = mutableListOf<HoursBlock>()
        val used = mutableSetOf<HoursBlockType>()

        fun replacement(type: HoursBlockType): List<HoursBlock> {
            val fromProper = properByType[type].orEmpty()
            if (fromProper.isNotEmpty()) return fromProper
            return commonByType[type].orEmpty()
        }

        temporal.blocks.forEach { block ->
            when {
                block.type == HoursBlockType.PSALMODY && properHasPsalmody -> {
                    if (used.add(HoursBlockType.PSALMODY)) {
                        result += properByType[HoursBlockType.PSALMODY].orEmpty()
                    }
                }
                block.type in replaceable && replacement(block.type).isNotEmpty() -> {
                    if (used.add(block.type)) result += replacement(block.type)
                }
                else -> result += block
            }
        }

        // Append only explicit liturgical roles absent from the temporal skeleton.
        // Generic TEXT/HEADING blocks stay source-local because their provenance
        // cannot be established safely outside their original document.
        replaceable.forEach { type ->
            if (type !in used && temporal.blocks.none { it.type == type }) {
                result += replacement(type)
            }
        }

        val preferredTitle = proper?.title?.takeIf { it.isNotBlank() }
            ?: common?.title?.takeIf { it.isNotBlank() }
            ?: temporal.title.takeIf { it.isNotBlank() }
            ?: hourKey
        return HoursNativeDocument(preferredTitle, result)
    }

    /** Base common + proper overlay for feasts/solemnities explicitly tied to a common. */
    private fun overlay(
        base: HoursNativeDocument,
        proper: HoursNativeDocument,
        hourKey: String,
        allowPsalmody: Boolean
    ): HoursNativeDocument {
        val replaceable = mutableSetOf(
            HoursBlockType.HYMN,
            HoursBlockType.ANTIPHON,
            HoursBlockType.READING,
            HoursBlockType.RESPONSORY,
            HoursBlockType.CANTICLE,
            HoursBlockType.INTERCESSIONS,
            HoursBlockType.PRAYER
        )
        if (allowPsalmody) replaceable += HoursBlockType.PSALMODY
        val properByType = proper.blocks.groupBy { it.type }
        val used = mutableSetOf<HoursBlockType>()
        val result = mutableListOf<HoursBlock>()

        base.blocks.forEach { block ->
            val replacement = properByType[block.type].orEmpty()
            if (block.type in replaceable && replacement.isNotEmpty()) {
                if (used.add(block.type)) result += replacement
            } else result += block
        }
        replaceable.forEach { type ->
            if (type !in used && base.blocks.none { it.type == type }) {
                result += properByType[type].orEmpty()
            }
        }
        return HoursNativeDocument(
            proper.title.ifBlank { base.title.ifBlank { hourKey } },
            result
        )
    }

    private fun empty(hourKey: String) = HoursNativeDocument(
        hourKey.ifBlank { "Liturgia de las Horas" },
        emptyList()
    )
}
