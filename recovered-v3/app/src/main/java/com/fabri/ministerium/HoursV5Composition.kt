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

        if (key == "office") {
            return composeOfficeOfReadings(normalizedRank, temporal, proper, common)
        }
        if (key == "invitatory") {
            return composeInvitatory(normalizedRank, temporal, proper, common)
        }
        if (key == "terce" || key == "sext" || key == "none") {
            return composeIntermediateHour(key, normalizedRank, temporal, proper, common)
        }

        if (!hasProper && !hasCommon) return temporal ?: empty(key)

        if (normalizedRank == "F" || normalizedRank == "S") {
            return when {
                hasCommon && hasProper -> overlay(common!!, proper!!, key, allowPsalmody = true)
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty(key)
            }
        }

        if (normalizedRank == "M" || normalizedRank == "m" || normalizedRank == "m*") {
            if (!hasTemporal) return when {
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty(key)
            }
            return composeMemory(key, temporal!!, proper, common)
        }

        return when {
            hasProper -> proper!!
            hasCommon -> common!!
            hasTemporal -> temporal!!
            else -> empty(key)
        }
    }

    @JvmStatic
    fun compose(
        hourKey: String?,
        rank: String?,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?
    ): HoursNativeDocument = compose(hourKey, rank, temporal, proper, null)

    /**
     * Invitatory keeps its psalm/invitatory body from the resolved temporal source.
     * A saint or explicit common may replace only the invitatory antiphon. This
     * prevents a proper entry from dragging an unrelated psalmody or neighbouring
     * celebration into the beginning of the office.
     */
    private fun composeInvitatory(
        rank: String,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?,
        common: HoursNativeDocument?
    ): HoursNativeDocument {
        val hasTemporal = temporal != null && temporal.blocks.isNotEmpty()
        val hasProper = proper != null && proper.blocks.isNotEmpty()
        val hasCommon = common != null && common.blocks.isNotEmpty()

        if (!hasTemporal) {
            return when {
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty("invitatory")
            }
        }
        if (!hasProper && !hasCommon) return temporal!!

        val source = proper ?: common
        val antiphon = source?.blocks?.firstOrNull { it.type == HoursBlockType.ANTIPHON }
        if (antiphon == null) return temporal!!

        val result = temporal!!.blocks.toMutableList()
        val index = result.indexOfFirst { it.type == HoursBlockType.ANTIPHON }
        if (index >= 0) {
            result[index] = antiphon
        } else {
            result.add(0, antiphon)
        }
        val title = source.title.takeIf { it.isNotBlank() } ?: temporal.title
        return HoursNativeDocument(title, result)
    }

    /**
     * Tercia, Sexta and Nona use a strict source policy:
     * - memorials retain the feria psalmody and may replace only reading/prayer;
     * - feasts/solemnities use their verified proper or explicitly selected common;
     * - an incomplete major celebration is never silently repaired from the feria.
     */
    private fun composeIntermediateHour(
        hourKey: String,
        rank: String,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?,
        common: HoursNativeDocument?
    ): HoursNativeDocument {
        val hasTemporal = temporal != null && temporal.blocks.isNotEmpty()
        val hasProper = proper != null && proper.blocks.isNotEmpty()
        val hasCommon = common != null && common.blocks.isNotEmpty()

        if (rank == "F" || rank == "S") {
            return when {
                hasProper && hasCommon -> overlay(common!!, proper!!, hourKey, allowPsalmody = true)
                hasProper -> proper!!
                hasCommon -> common!!
                else -> empty(hourKey)
            }
        }

        if (rank == "M" || rank == "m" || rank == "m*") {
            if (!hasTemporal) {
                return when {
                    hasProper -> proper!!
                    hasCommon -> common!!
                    else -> empty(hourKey)
                }
            }
            val result = temporal!!.blocks.toMutableList()
            val sourceReading = proper?.blocks?.firstOrNull { it.type == HoursBlockType.READING }
                ?: common?.blocks?.firstOrNull { it.type == HoursBlockType.READING }
            val sourceResponsory = proper?.blocks?.firstOrNull { it.type == HoursBlockType.RESPONSORY }
                ?: common?.blocks?.firstOrNull { it.type == HoursBlockType.RESPONSORY }
            val sourcePrayer = proper?.blocks?.lastOrNull { it.type == HoursBlockType.PRAYER }
                ?: common?.blocks?.lastOrNull { it.type == HoursBlockType.PRAYER }

            replaceFirst(result, HoursBlockType.READING, sourceReading)
            replaceFirst(result, HoursBlockType.RESPONSORY, sourceResponsory)
            replaceFirst(result, HoursBlockType.PRAYER, sourcePrayer)

            val title = proper?.title?.takeIf { it.isNotBlank() }
                ?: common?.title?.takeIf { it.isNotBlank() }
                ?: temporal.title
            return HoursNativeDocument(title, result)
        }

        return when {
            hasProper -> proper!!
            hasCommon -> common!!
            hasTemporal -> temporal!!
            else -> empty(hourKey)
        }
    }

    /**
     * Office of Readings has a stricter contract than the other hours:
     * - memorial: first reading + first responsory remain temporal;
     * - second reading + its responsory come from the selected saint when present;
     * - prayer comes from the proper first, explicit common second;
     * - a feast/solemnity never borrows an incomplete reading cycle from the feria.
     */
    private fun composeOfficeOfReadings(
        rank: String,
        temporal: HoursNativeDocument?,
        proper: HoursNativeDocument?,
        common: HoursNativeDocument?
    ): HoursNativeDocument {
        val hasTemporal = temporal != null && temporal.blocks.isNotEmpty()
        val hasProper = proper != null && proper.blocks.isNotEmpty()
        val hasCommon = common != null && common.blocks.isNotEmpty()
        if (!hasProper && !hasCommon) return temporal ?: empty("office")

        if (rank == "F" || rank == "S") {
            return when {
                hasProper && completeOfficeCycle(proper!!) -> proper
                hasCommon && hasProper -> overlayOfficeCommon(common!!, proper!!)
                hasCommon -> common!!
                hasProper -> proper!!
                else -> empty("office")
            }
        }

        if (rank != "M" && rank != "m" && rank != "m*") {
            return when {
                hasProper -> proper!!
                hasCommon -> common!!
                hasTemporal -> temporal!!
                else -> empty("office")
            }
        }

        if (!hasTemporal) return when {
            hasProper -> proper!!
            hasCommon -> common!!
            else -> empty("office")
        }

        val temporalBlocks = temporal!!.blocks.toMutableList()
        val properSecond = proper?.blocks?.firstOrNull { it.type == HoursBlockType.SECOND_READING }
            ?: proper?.blocks?.lastOrNull { it.type == HoursBlockType.READING }
        val commonSecond = common?.blocks?.firstOrNull { it.type == HoursBlockType.SECOND_READING }
            ?: common?.blocks?.lastOrNull { it.type == HoursBlockType.READING }
        val secondReading = properSecond ?: commonSecond

        val secondIndex = temporalBlocks.indexOfFirst { it.type == HoursBlockType.SECOND_READING }
            .takeIf { it >= 0 }
            ?: temporalBlocks.indexOfLast { it.type == HoursBlockType.READING }
        if (secondReading != null && secondIndex >= 0) {
            temporalBlocks[secondIndex] = secondReading.copy(type = HoursBlockType.SECOND_READING)

            val replacementResponsory = responsoryAfterSecondReading(proper)
                ?: responsoryAfterSecondReading(common)
            val temporalResponsory = temporalBlocks.indices.firstOrNull { index ->
                index > secondIndex && temporalBlocks[index].type == HoursBlockType.RESPONSORY
            }
            if (replacementResponsory != null && temporalResponsory != null) {
                temporalBlocks[temporalResponsory] = replacementResponsory
            }
        }

        replaceFirst(temporalBlocks, HoursBlockType.HYMN,
            proper?.blocks?.firstOrNull { it.type == HoursBlockType.HYMN }
                ?: common?.blocks?.firstOrNull { it.type == HoursBlockType.HYMN })
        replaceFirst(temporalBlocks, HoursBlockType.PRAYER,
            proper?.blocks?.lastOrNull { it.type == HoursBlockType.PRAYER }
                ?: common?.blocks?.lastOrNull { it.type == HoursBlockType.PRAYER })

        val title = proper?.title?.takeIf { it.isNotBlank() }
            ?: common?.title?.takeIf { it.isNotBlank() }
            ?: temporal.title
        return HoursNativeDocument(title, temporalBlocks)
    }

    private fun completeOfficeCycle(document: HoursNativeDocument): Boolean {
        val first = document.blocks.any { it.type == HoursBlockType.FIRST_READING }
        val second = document.blocks.any { it.type == HoursBlockType.SECOND_READING }
        val responsories = document.blocks.count { it.type == HoursBlockType.RESPONSORY }
        return first && second && responsories >= 2
    }

    private fun responsoryAfterSecondReading(document: HoursNativeDocument?): HoursBlock? {
        if (document == null) return null
        val blocks = document.blocks
        var second = blocks.indexOfFirst { it.type == HoursBlockType.SECOND_READING }
        if (second < 0) second = blocks.indexOfLast { it.type == HoursBlockType.READING }
        if (second < 0) return null
        return blocks.drop(second + 1).firstOrNull { it.type == HoursBlockType.RESPONSORY }
    }

    private fun overlayOfficeCommon(
        common: HoursNativeDocument,
        proper: HoursNativeDocument
    ): HoursNativeDocument {
        val result = common.blocks.toMutableList()
        replaceFirst(result, HoursBlockType.HYMN,
            proper.blocks.firstOrNull { it.type == HoursBlockType.HYMN })
        replaceFirst(result, HoursBlockType.FIRST_READING,
            proper.blocks.firstOrNull { it.type == HoursBlockType.FIRST_READING })
        replaceFirst(result, HoursBlockType.SECOND_READING,
            proper.blocks.firstOrNull { it.type == HoursBlockType.SECOND_READING })
        replaceFirst(result, HoursBlockType.PRAYER,
            proper.blocks.lastOrNull { it.type == HoursBlockType.PRAYER })

        val properSecondResponsory = responsoryAfterSecondReading(proper)
        val secondIndex = result.indexOfFirst { it.type == HoursBlockType.SECOND_READING }
        if (properSecondResponsory != null && secondIndex >= 0) {
            val responsoryIndex = result.indices.firstOrNull { index ->
                index > secondIndex && result[index].type == HoursBlockType.RESPONSORY
            }
            if (responsoryIndex != null) result[responsoryIndex] = properSecondResponsory
        }
        return HoursNativeDocument(proper.title.ifBlank { common.title }, result)
    }

    private fun replaceFirst(
        blocks: MutableList<HoursBlock>,
        type: HoursBlockType,
        replacement: HoursBlock?
    ) {
        if (replacement == null) return
        val index = blocks.indexOfFirst { it.type == type }
        if (index >= 0) blocks[index] = replacement
    }

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
            HoursBlockType.GOSPEL_ANTIPHON,
            HoursBlockType.READING,
            HoursBlockType.FIRST_READING,
            HoursBlockType.SECOND_READING,
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

    private fun overlay(
        base: HoursNativeDocument,
        proper: HoursNativeDocument,
        hourKey: String,
        allowPsalmody: Boolean
    ): HoursNativeDocument {
        val replaceable = mutableSetOf(
            HoursBlockType.HYMN,
            HoursBlockType.ANTIPHON,
            HoursBlockType.GOSPEL_ANTIPHON,
            HoursBlockType.READING,
            HoursBlockType.FIRST_READING,
            HoursBlockType.SECOND_READING,
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
