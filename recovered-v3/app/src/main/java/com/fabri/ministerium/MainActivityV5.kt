package com.fabri.ministerium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/** Main Ministerium 5 shell. Keeps every stable module reachable during migration. */
class MainActivityV5 : ThemedActivity() {

    private var appliedThemeMode = ""
    private var dark = false
    private var bg = 0
    private var cardColor = 0
    private var ink = 0
    private var muted = 0
    private var wine = 0
    private var gold = 0
    private lateinit var continueSection: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedThemeMode = ThemeUtils.getMode(this)
        ThemeUtils.apply(this)
        super.onCreate(savedInstanceState)

        dark = ThemeUtils.isDark(this)
        bg = Color.parseColor(if (dark) "#12100F" else "#F7F2EA")
        cardColor = Color.parseColor(if (dark) "#211C1A" else "#FFFDF9")
        ink = Color.parseColor(if (dark) "#F5EEE8" else "#2B211E")
        muted = Color.parseColor(if (dark) "#BFAFA8" else "#756863")
        wine = Color.parseColor(if (dark) "#D89AA3" else "#6D1E2B")
        gold = Color.parseColor("#D2A84A")

        // Preserve behavior that existed in the stable shell. These calls are
        // idempotent and keep reminders/focus recovery alive after the V5 switch.
        PrayerFocusController.recoverStaleSession(this)
        PrayerReminderScheduler.restore(this)
        GospelReminderScheduler.restore(this)
        BiblePlanReminderScheduler.restore(this)

        setContentView(buildUi())
        bindContinueReading()
    }

    override fun onResume() {
        super.onResume()
        if (appliedThemeMode != ThemeUtils.getMode(this)) {
            recreate()
            return
        }
        if (::continueSection.isInitialized) bindContinueReading()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(32))
        }
        scroll.addView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(18))
        }
        root.addView(header, matchWrap())

        header.addView(text("✠", 28f, wine, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = rounded(gold, 16)
            contentDescription = "Ministerium"
        }, LinearLayout.LayoutParams(dp(52), dp(52)))

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(text("MINISTERIUM", 24f, ink, Typeface.BOLD))
            addView(text("Oración, liturgia y estudio de la fe", 12f, muted, Typeface.NORMAL))
        }
        header.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        header.addView(text(if (dark) "☀" else "◐", 22f, ink, Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            contentDescription = if (dark) "Activar modo claro" else "Activar modo oscuro"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                ThemeUtils.setMode(this@MainActivityV5, if (dark) ThemeUtils.LIGHT else ThemeUtils.DARK)
                recreate()
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        root.addView(text("¿Qué quieres consultar?", 26f, ink, Typeface.BOLD), matchWrap())
        root.addView(text(
            "Todos los recursos estables siguen disponibles mientras avanza la interfaz 5.0.",
            14f,
            muted,
            Typeface.NORMAL
        ).apply {
            setPadding(0, dp(6), 0, dp(12))
        }, matchWrap())

        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        quick.addView(quickAction("Buscar") {
            startActivity(Intent(this, SearchActivity::class.java))
        }, quickParams(0, 4))
        quick.addView(quickAction("Favoritos") {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }, quickParams(4, 4))
        quick.addView(quickAction("Avisos") { openReminders() }, quickParams(4, 0))
        root.addView(quick, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, dp(12))
        })

        continueSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(continueSection, matchWrap())

        root.addView(sectionLabel("LITURGIA Y ESCRITURA"))
        root.addView(card("B", "Biblia", "Biblia disponible sin conexión") {
            startActivity(Intent(this, BibleActivity::class.java))
        })
        root.addView(card("M", "Misal", "Celebración del día y formularios") {
            startActivity(Intent(this, MissalV5Activity::class.java))
        })
        root.addView(card("H", "Liturgia de las Horas", "Oficio del día · lector nativo") { openToday() })
        root.addView(card("L", "Lecturas de la Misa", "Leccionario y sincronización local") {
            startActivity(Intent(this, MassReadingsActivity::class.java))
        })
        root.addView(card("C", "Calendario litúrgico", "Celebraciones, colores y calendario de Ecuador") {
            startActivity(Intent(this, LiturgicalCalendarActivity::class.java))
        })
        root.addView(card("LA", "Liturgia Horarum", "Liturgia de las Horas en latín") {
            startActivity(Intent(this, LatinHoursActivity::class.java))
        })

        root.addView(sectionLabel("ORACIÓN Y PASTORAL"))
        root.addView(card("O", "Oraciones", "Oraciones básicas y recursos de oración") {
            startActivity(Intent(this, BasicPrayersActivity::class.java))
        })
        root.addView(card("D", "Devocionario", "Devociones, examen y oración personal") {
            startActivity(Intent(this, DevotionalHubActivity::class.java))
        })
        root.addView(card("R", "Rituales", "Bautismo, enfermos, Viático y exequias") {
            startActivity(Intent(this, PastoralActivity::class.java))
        })
        root.addView(card("+", "Bendicional", "Bendiciones para diversas circunstancias") {
            openBlessings()
        })

        root.addView(sectionLabel("FORMACIÓN Y ESTUDIO"))
        root.addView(card("M", "Magisterio y Derecho", "Documentos, catecismo y derecho canónico") {
            startActivity(Intent(this, MagisteriumActivity::class.java))
        })
        root.addView(card("E", "Mi estudio", "Notas, reflexiones y material personal") {
            startActivity(Intent(this, MyStudyActivity::class.java))
        })

        root.addView(sectionLabel("APLICACIÓN"))
        root.addView(card("⚙", "Ajustes", "Tema, lectura, márgenes, recordatorios y actualizaciones") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })

        return scroll
    }

    private fun bindContinueReading() {
        if (!::continueSection.isInitialized) return
        continueSection.removeAllViews()
        val entry = ContinueReadingStore.latest(this)
        if (entry == null) {
            continueSection.visibility = View.GONE
            return
        }
        continueSection.visibility = View.VISIBLE
        continueSection.addView(sectionLabel("CONTINUAR LEYENDO"))
        continueSection.addView(card("›", entry.title, entry.module + if (entry.scrollY > 0) " · posición guardada" else "") {
            if (!ContinueReadingStore.open(this, entry)) {
                continueSection.visibility = View.GONE
            }
        })
    }

    private fun quickAction(label: String, action: () -> Unit) = text(label, 14f, wine, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        background = rounded(cardColor, 14)
        minHeight = dp(48)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun quickParams(left: Int, right: Int) = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
        setMargins(dp(left), 0, dp(right), 0)
    }

    private fun sectionLabel(value: String) = text(value, 11f, wine, Typeface.BOLD).apply {
        letterSpacing = .10f
        setPadding(dp(2), dp(14), 0, dp(6))
    }

    private fun card(icon: String, title: String, subtitle: String, action: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(cardColor, 16)
            elevation = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(5), 0, dp(5)) }
            isClickable = true
            isFocusable = true
            contentDescription = if (subtitle.isBlank()) title else "$title. $subtitle"
            setOnClickListener { action() }
        }

        row.addView(text(icon, if (icon.length > 1) 14f else 20f, wine, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = rounded(
                if (dark) Color.parseColor("#332821") else Color.parseColor("#F2E2B9"),
                14
            )
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
            addView(text(title, 17f, ink, Typeface.BOLD))
            if (subtitle.isNotBlank()) {
                addView(text(subtitle, 13f, muted, Typeface.NORMAL).apply {
                    setPadding(0, dp(3), 0, 0)
                })
            }
        }
        row.addView(body, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(text("›", 28f, gold, Typeface.NORMAL), wrapWrap())
        return row
    }

    private fun openToday() {
        val today = Calendar.getInstance()
        startActivity(Intent(this, HoursV5Activity::class.java).apply {
            putExtra(HoursV5Activity.EXTRA_YEAR, today.get(Calendar.YEAR))
            putExtra(HoursV5Activity.EXTRA_MONTH, today.get(Calendar.MONTH))
            putExtra(HoursV5Activity.EXTRA_DAY, today.get(Calendar.DAY_OF_MONTH))
        })
    }

    private fun openReminders() {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.EXTRA_OPEN_REMINDERS, true)
        })
    }

    private fun openBlessings() {
        startActivity(Intent(this, RitualCatalogActivity::class.java).apply {
            putExtra(RitualCatalogActivity.EXTRA_DOCUMENT_ID, RitualRepository.COMMON_BLESSINGS_ID)
        })
    }

    private fun text(value: String, sp: Float, color: Int, style: Int) = TextView(this).apply {
        text = value
        textSize = sp
        setTextColor(color)
        setTypeface(Typeface.DEFAULT, style)
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (color == cardColor) {
            setStroke(dp(1), if (dark) Color.parseColor("#3A312E") else Color.parseColor("#E8DED4"))
        }
    }

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun wrapWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
