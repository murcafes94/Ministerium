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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private lateinit var todayLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedThemeMode = ThemeUtils.getMode(this)
        ThemeUtils.apply(this)
        super.onCreate(savedInstanceState)

        dark = ThemeUtils.isDark(this)
        bg = resources.getColor(R.color.cream)
        cardColor = resources.getColor(R.color.paper)
        ink = resources.getColor(R.color.ink)
        muted = resources.getColor(R.color.muted)
        wine = resources.getColor(R.color.wine)
        gold = resources.getColor(R.color.gold)

        // Preserve behavior that existed in the stable shell. These calls are
        // idempotent and keep reminders/focus recovery alive after the V5 switch.
        PrayerFocusController.recoverStaleSession(this)
        PrayerReminderScheduler.restore(this)
        GospelReminderScheduler.restore(this)
        BiblePlanReminderScheduler.restore(this)

        setContentView(buildUi())
        bindContinueReading()
        refreshTodayLabel()
    }

    override fun onResume() {
        super.onResume()
        if (appliedThemeMode != ThemeUtils.getMode(this)) {
            recreate()
            return
        }
        if (::continueSection.isInitialized) bindContinueReading()
        if (::todayLabel.isInitialized) refreshTodayLabel()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(32))
        }
        NativeUi.addCenteredRoot(scroll, root, 1040)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(16))
        }
        header.addView(icon(R.drawable.ic_cross_41, wine).apply {
            background = rounded(if (dark) Color.parseColor("#403023") else Color.parseColor("#F0DFC0"), 16)
            contentDescription = "Ministerium"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(4), 0)
            addView(text("Ministerium", 25f, ink, Typeface.BOLD).apply { typeface = Typeface.create("serif", Typeface.BOLD) })
            addView(text("Oración, liturgia y estudio", 12f, muted, Typeface.NORMAL))
        }
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(icon(R.drawable.ic_theme, wine).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = if (dark) "Activar modo claro" else "Activar modo oscuro"
            setBackgroundResource(R.drawable.bg_button_secondary)
            isFocusable = true
            setOnClickListener {
                ThemeUtils.setMode(this@MainActivityV5, if (dark) ThemeUtils.LIGHT else ThemeUtils.DARK)
                recreate()
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header, matchWrap())

        val today = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(resources.getColor(R.color.wine_dark), 22)
        }
        todayLabel = text("", 12f, Color.parseColor("#E8D5BD"), Typeface.BOLD)
        today.addView(todayLabel)
        today.addView(text("Tu oración de hoy", 26f, Color.WHITE, Typeface.BOLD).apply {
            setPadding(0, dp(8), 0, dp(6))
            typeface = Typeface.create("serif", Typeface.BOLD)
        })
        today.addView(text("Un espacio para rezar, leer y profundizar en la fe.", 14f, Color.parseColor("#EFE4DA"), Typeface.NORMAL))
        val dailyActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(16), 0, 0) }
        dailyActions.addView(quickAction("Horas de hoy") { openToday() }, quickParams(0, 5))
        dailyActions.addView(quickAction("Leccionario") { startActivity(Intent(this, MassReadingsActivity::class.java)) }, quickParams(5, 0))
        today.addView(dailyActions)
        root.addView(today, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })

        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        quick.addView(quickAction("Buscar", R.drawable.ic_search) { startActivity(Intent(this, SearchActivity::class.java)) }, quickParams(0, 4))
        quick.addView(quickAction("Favoritos", R.drawable.ic_star_41) { startActivity(Intent(this, FavoritesActivity::class.java)) }, quickParams(4, 4))
        quick.addView(quickAction("Avisos", R.drawable.ic_bell_41) { openReminders() }, quickParams(4, 0))
        root.addView(quick, matchWrap())
        continueSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        root.addView(continueSection, matchWrap())

        addModules(root, "LITURGIA Y ESCRITURA", listOf(
            card(R.drawable.ic_book_41, "Biblia", "Escritura y planes de lectura") { startActivity(Intent(this, BibleActivity::class.java)) },
            card(R.drawable.ic_sun_41, "Liturgia de las Horas", "Oración del día") { openToday() },
            card(R.drawable.ic_document_41, "Leccionario", "Lecturas de la Misa por fecha") { startActivity(Intent(this, MassReadingsActivity::class.java)) },
            card(R.drawable.ic_calendar, "Calendario litúrgico", "Celebraciones de Ecuador") { startActivity(Intent(this, LiturgicalCalendarActivity::class.java)) },
            card(R.drawable.ic_book_41, "Liturgia Horarum", "Liturgia de las Horas en latín") { startActivity(Intent(this, LatinHoursActivity::class.java)) }
        ))
        addModules(root, "ORACIÓN Y PASTORAL", listOf(
            card(R.drawable.ic_cross_41, "Oraciones", "Oraciones básicas") { startActivity(Intent(this, BasicPrayersActivity::class.java)) },
            card(R.drawable.ic_star_41, "Devocionario", "Devociones y oración personal") { startActivity(Intent(this, DevotionalHubActivity::class.java)) },
            card(R.drawable.ic_cross_41, "Rituales", "Sacramentos y atención pastoral") { startActivity(Intent(this, PastoralActivity::class.java)) },
            card(R.drawable.ic_cross_41, "Bendicional", "Bendiciones") { openBlessings() }
        ))
        addModules(root, "FORMACIÓN Y ESTUDIO", listOf(
            card(R.drawable.ic_document_41, "Magisterio y Derecho", "Documentos, catecismo y derecho canónico") { startActivity(Intent(this, MagisteriumActivity::class.java)) },
            card(R.drawable.ic_edit_41, "Mi estudio", "Notas y reflexiones personales") { startActivity(Intent(this, MyStudyActivity::class.java)) }
        ))
        root.addView(sectionLabel("APLICACIÓN"))
        root.addView(card(R.drawable.ic_settings_41, "Ajustes", "Apariencia, lectura, respaldos y recordatorios") { startActivity(Intent(this, SettingsActivity::class.java)) })
        return scroll
    }

    private fun refreshTodayLabel() {
        todayLabel.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "EC")).format(Calendar.getInstance().time)
    }

    private fun addModules(root: LinearLayout, title: String, cards: List<View>) {
        root.addView(sectionLabel(title))
        // Large text keeps one column so labels do not become squeezed on tablets.
        if (resources.configuration.screenWidthDp < 600 || resources.configuration.fontScale > 1.3f) {
            cards.forEach { root.addView(it) }
            return
        }
        cards.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
            pair.forEachIndexed { index, view ->
                row.addView(view, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(if (index == 0) 0 else dp(6), dp(5), if (index == 0) dp(6) else 0, dp(5)) })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            root.addView(row, matchWrap())
        }
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
        continueSection.addView(card(R.drawable.ic_book_41, entry.title, entry.module + if (entry.scrollY > 0) " · posición guardada" else "") {
            if (!ContinueReadingStore.open(this, entry)) {
                continueSection.visibility = View.GONE
            }
        })
    }

    private fun quickAction(label: String, iconRes: Int = 0, action: () -> Unit) = text(label, 14f, wine, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_button_secondary)
        minHeight = dp(52)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        if (iconRes != 0) {
            val drawable = resources.getDrawable(iconRes, theme).mutate()
            drawable.setTint(wine)
            drawable.setBounds(0, 0, dp(18), dp(18))
            setCompoundDrawables(null, drawable, null, null)
            compoundDrawablePadding = dp(3)
        }
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun quickParams(left: Int, right: Int) = LinearLayout.LayoutParams(0, -2, 1f).apply {
        setMargins(dp(left), 0, dp(right), 0)
    }

    private fun sectionLabel(value: String) = text(value, 11f, wine, Typeface.BOLD).apply {
        letterSpacing = .10f
        setPadding(dp(2), dp(14), 0, dp(6))
    }

    private fun card(iconRes: Int, title: String, subtitle: String, action: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            setBackgroundResource(R.drawable.bg_card)
            minimumHeight = dp(94)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(5), 0, dp(5)) }
            isClickable = true
            isFocusable = true
            contentDescription = if (subtitle.isBlank()) title else "$title. $subtitle"
            setOnClickListener { action() }
        }

        row.addView(icon(iconRes, wine).apply {
            background = rounded(if (dark) Color.parseColor("#403023") else Color.parseColor("#F0DFC0"), 14)
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
        row.addView(icon(R.drawable.ic_chevron_right, wine).apply { setPadding(0, 0, 0, 0) }, LinearLayout.LayoutParams(dp(24), dp(24)))
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

    private fun icon(resource: Int, tint: Int) = ImageView(this).apply {
        setImageResource(resource)
        setColorFilter(tint)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
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
