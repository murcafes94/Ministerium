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
    val type: MassElementType,
    val title: String,
    val text: String = "",
    val options: List<String> = emptyList()
)

data class MassSection(
    val id: String,
    val title: String,
    val summary: String,
    val elements: List<MassElement> = emptyList()
)

object MissalV5Semantic {
    @JvmStatic
    fun sections(): List<MassSection> = listOf(
        MassSection(
            id = "initial",
            title = "Ritos iniciales",
            summary = "Entrada, saludo, acto penitencial, Gloria y colecta",
            elements = listOf(
                MassElement(MassElementType.ANTIPHON, "Antífona de entrada"),
                MassElement(MassElementType.CELEBRANT, "Saludo"),
                MassElement(
                    MassElementType.OPTION,
                    "Acto penitencial",
                    options = listOf("I", "II", "III")
                ),
                MassElement(MassElementType.PRAYER, "Gloria"),
                MassElement(MassElementType.PRAYER, "Oración colecta")
            )
        ),
        MassSection(
            id = "word",
            title = "Liturgia de la Palabra",
            summary = "Lecturas del día, salmo, Evangelio, Credo y oración universal",
            elements = listOf(
                MassElement(MassElementType.READING_REFERENCE, "Primera lectura"),
                MassElement(MassElementType.READING_REFERENCE, "Salmo responsorial"),
                MassElement(MassElementType.READING_REFERENCE, "Evangelio"),
                MassElement(MassElementType.PRAYER, "Credo"),
                MassElement(MassElementType.PRAYER, "Oración universal")
            )
        ),
        MassSection(
            id = "eucharist",
            title = "Liturgia eucarística",
            summary = "Preparación de los dones, prefacio y plegaria eucarística",
            elements = listOf(
                MassElement(MassElementType.RUBRIC, "Preparación de los dones"),
                MassElement(MassElementType.PRAYER, "Oración sobre las ofrendas"),
                MassElement(MassElementType.PRAYER, "Prefacio"),
                MassElement(
                    MassElementType.OPTION,
                    "Plegaria eucarística",
                    options = listOf("I", "II", "III", "IV")
                )
            )
        ),
        MassSection(
            id = "communion",
            title = "Rito de la comunión",
            summary = "Padrenuestro, paz, fracción, comunión y oración",
            elements = listOf(
                MassElement(MassElementType.PRAYER, "Padrenuestro"),
                MassElement(MassElementType.RUBRIC, "Rito de la paz"),
                MassElement(MassElementType.RUBRIC, "Fracción del pan"),
                MassElement(MassElementType.ANTIPHON, "Antífona de comunión"),
                MassElement(MassElementType.PRAYER, "Oración después de la comunión")
            )
        ),
        MassSection(
            id = "conclusion",
            title = "Rito de conclusión",
            summary = "Bendición y despedida",
            elements = listOf(
                MassElement(MassElementType.CELEBRANT, "Bendición"),
                MassElement(MassElementType.CELEBRANT, "Despedida")
            )
        ),
        MassSection(
            id = "other",
            title = "Otros formularios",
            summary = "Comunes, necesidades, votivas, difuntos y santos"
        )
    )
}
