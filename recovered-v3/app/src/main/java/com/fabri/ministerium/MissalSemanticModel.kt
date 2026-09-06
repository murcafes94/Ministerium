package com.fabri.ministerium

enum class MassElementType {
    TITLE,
    RUBRIC,
    CELEBRANT,
    ASSEMBLY,
    PRAYER,
    READING_REFERENCE,
    OPTION,
    ANTIPHON
}

data class MassElement(
    val id: String,
    val type: MassElementType,
    val title: String,
    val text: String = "",
    val options: List<String> = emptyList(),
    val required: Boolean = true
)

data class MassSection(
    val id: String,
    val title: String,
    val summary: String,
    val elements: List<MassElement> = emptyList()
)

object MissalV5Semantic {
    private val sections = listOf(
        MassSection(
            id = "initial",
            title = "Ritos iniciales",
            summary = "Entrada, saludo, acto penitencial, Gloria y colecta",
            elements = listOf(
                MassElement("entrance_antiphon", MassElementType.ANTIPHON, "Antífona de entrada"),
                MassElement("greeting", MassElementType.CELEBRANT, "Saludo"),
                MassElement(
                    id = "penitential_act",
                    type = MassElementType.OPTION,
                    title = "Acto penitencial",
                    options = listOf("I", "II", "III")
                ),
                MassElement("gloria", MassElementType.PRAYER, "Gloria", required = false),
                MassElement("collect", MassElementType.PRAYER, "Oración colecta")
            )
        ),
        MassSection(
            id = "word",
            title = "Liturgia de la Palabra",
            summary = "Lecturas del día, salmo, Evangelio, Credo y oración universal",
            elements = listOf(
                MassElement("first_reading", MassElementType.READING_REFERENCE, "Primera lectura"),
                MassElement("psalm", MassElementType.READING_REFERENCE, "Salmo responsorial"),
                MassElement("second_reading", MassElementType.READING_REFERENCE, "Segunda lectura", required = false),
                MassElement("gospel", MassElementType.READING_REFERENCE, "Evangelio"),
                MassElement("creed", MassElementType.PRAYER, "Credo", required = false),
                MassElement("universal_prayer", MassElementType.PRAYER, "Oración universal")
            )
        ),
        MassSection(
            id = "eucharist",
            title = "Liturgia eucarística",
            summary = "Preparación de los dones, prefacio y plegaria eucarística",
            elements = listOf(
                MassElement("gifts", MassElementType.RUBRIC, "Preparación de los dones"),
                MassElement("offerings", MassElementType.PRAYER, "Oración sobre las ofrendas"),
                MassElement("preface", MassElementType.PRAYER, "Prefacio"),
                MassElement(
                    id = "eucharistic_prayer",
                    type = MassElementType.OPTION,
                    title = "Plegaria eucarística",
                    options = listOf("I", "II", "III", "IV")
                )
            )
        ),
        MassSection(
            id = "communion",
            title = "Rito de la comunión",
            summary = "Padrenuestro, paz, fracción, comunión y oración",
            elements = listOf(
                MassElement("our_father", MassElementType.PRAYER, "Padrenuestro"),
                MassElement("peace", MassElementType.RUBRIC, "Rito de la paz"),
                MassElement("fraction", MassElementType.RUBRIC, "Fracción del pan"),
                MassElement("communion_antiphon", MassElementType.ANTIPHON, "Antífona de comunión"),
                MassElement("post_communion", MassElementType.PRAYER, "Oración después de la comunión")
            )
        ),
        MassSection(
            id = "conclusion",
            title = "Rito de conclusión",
            summary = "Bendición y despedida",
            elements = listOf(
                MassElement("blessing", MassElementType.CELEBRANT, "Bendición"),
                MassElement("dismissal", MassElementType.CELEBRANT, "Despedida")
            )
        ),
        MassSection(
            id = "other",
            title = "Otros formularios",
            summary = "Comunes, necesidades, votivas, difuntos y santos"
        )
    )

    @JvmStatic
    fun sections(): List<MassSection> = sections

    @JvmStatic
    fun section(id: String?): MassSection? = sections.firstOrNull { it.id == id }

    @JvmStatic
    fun element(sectionId: String?, elementId: String): MassElement? =
        section(sectionId)?.elements?.firstOrNull { it.id == elementId }
}
