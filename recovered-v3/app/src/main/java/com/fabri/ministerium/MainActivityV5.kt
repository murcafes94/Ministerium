package com.fabri.ministerium

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/** Main Ministerium 5 shell. */
class MainActivityV5 : ThemedActivity() {

    private var dark = false
    private var bg = 0
    private var cardColor = 0
    private var ink = 0
    private var muted = 0
    private var wine = 0
    private var gold = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.apply(this)
        super.onCreate(savedInstanceState)
        dark = ThemeUtils.isDark(this)
        bg = Color.parseColor(if (dark) "#12100F" else "#F7F2EA")
        cardColor = Color.parseColor(if (dark) "#211C1A" else "#FFFDF9")
        ink = Color.parseColor(if (dark) "#F5EEE8" else "#2B211E")
        muted = Color.parseColor(if (dark) "#BFAFA8" else "#756863")
        wine = Color.parseColor(if (dark) "#D89AA3" else "#6D1E2B")
        gold = Color.parseColor("#D2A84A")
        setContentView(buildUi())
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
        scroll.addView(root, ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
        ))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(6), dp(4), dp(18))
        }
        root.addView(header, matchWrap())

        header.addView(text("✠", 28f, wine, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = rounded(gold, 16)
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
            setOnClickListener {
                ThemeUtils.setMode(this@MainActivityV5, if (dark) ThemeUtils.LIGHT else ThemeUtils.DARK)
                recreate()
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        root.addView(text("¿Qué quieres consultar?", 26f, ink, Typeface.BOLD), matchWrap())
        root.addView(text(
            "Accede directamente a los principales recursos de Ministerium.",
            14f,
            muted,
            Typeface.NORMAL
        ).apply {
            setPadding(0, dp(6), 0, dp(14))
        }, matchWrap())

        root.addView(card("B", "Biblia", "Biblia disponible sin conexión") {
            startActivity(Intent(this, BibleActivity::class.java))
        })
        root.addView(card("M", "Misal", "Celebración del día, lecturas y formularios") {
            startActivity(Intent(this, MissalV5Activity::class.java))
        })
        root.addView(card("☀", "Liturgia de las Horas", "Oficio del día") { openToday() })
        root.addView(card("✠", "Rituales y Bendicional", "Celebraciones, ritos y bendiciones") {
            startActivity(Intent(this, PastoralActivity::class.java))
        })
        root.addView(card("M", "Magisterio y Derecho", "Documentos, magisterio y derecho canónico") {
            startActivity(Intent(this, MagisteriumActivity::class.java))
        })
        root.addView(card("⚙", "Ajustes", "Tema, lectura, márgenes y preferencias") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })

        return scroll
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
            ).apply { setMargins(0, dp(6), 0, dp(6)) }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        row.addView(text(icon, 20f, wine, Typeface.BOLD).apply {
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
            addView(text(subtitle, 13f, muted, Typeface.NORMAL).apply {
                setPadding(0, dp(3), 0, 0)
            })
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
