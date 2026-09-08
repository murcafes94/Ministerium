package com.fabri.ministerium

data class HoursV5Item(
    val key: String,
    val title: String,
    val summary: String
)

object HoursV5Semantic {
    private val items = listOf(
        HoursV5Item("invitatory", "Invitatorio", "Salmo invitatorio y antífona"),
        HoursV5Item("office", "Oficio de lectura", "Himno, salmodia y lecturas"),
        HoursV5Item("lauds", "Laudes", "Oración de la mañana"),
        HoursV5Item("terce", "Tercia", "Hora intermedia"),
        HoursV5Item("sext", "Sexta", "Hora intermedia"),
        HoursV5Item("none", "Nona", "Hora intermedia"),
        HoursV5Item("vespers", "Vísperas", "Oración de la tarde"),
        HoursV5Item("compline", "Completas", "Oración antes del descanso nocturno")
    )

    @JvmStatic
    fun items(): List<HoursV5Item> = items

    @JvmStatic
    fun item(key: String?): HoursV5Item? = items.firstOrNull { it.key == key }
}
