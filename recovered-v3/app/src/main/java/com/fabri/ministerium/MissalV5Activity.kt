package com.fabri.ministerium

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Native Ministerium 5 Missal hub. */
class MissalV5Activity : ThemedActivity() {

    private val selectedDate: Calendar = Calendar.getInstance()
    private lateinit var dateView: TextView
    private lateinit var celebrationView: TextView
    private lateinit var detailView: TextView
    private lateinit var sections: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        refreshDay()
    }

    private fun buildScreen(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.cream))
        }

        val root = column().apply {
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }
        scroll.addView(root)

        root.addView(text("‹  Misal Diario Romano", 24, R.color.wine, true).apply {
            setPadding(0, dp(6), 0, dp(8))
            setOnClickListener { finish() }
        })

        root.addView(text("Misal · celebración del día", 13, R.color.muted, false).apply {
            setPadding(0, 0, 0, dp(18))
        })

        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        dateRow.addView(action("‹").apply {
            setOnClickListener { moveDay(-1) }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))

        dateView = text("", 18, R.color.ink, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { chooseDate() }
        }
        dateRow.addView(dateView, LinearLayout.LayoutParams(0, dp(52), 1f))

        dateRow.addView(action("›").apply {
            setOnClickListener { moveDay(1) }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))
        root.addView(dateRow)

        celebrationView = text("", 25, R.color.wine, true).apply {
            setPadding(0, dp(16), 0, dp(5))
        }
        root.addView(celebrationView)

        detailView = text("", 14, R.color.muted, false).apply {
            setPadding(0, 0, 0, dp(22))
        }
        root.addView(detailView)

        root.addView(text("CELEBRACIÓN", 12, R.color.wine, true).apply {
            letterSpacing = .11f
            setPadding(0, 0, 0, dp(10))
        })

        sections = column()
        root.addView(sections)
        return scroll
    }

    private fun refreshDay() {
        val formatted = SimpleDateFormat(
            "EEEE, d 'de' MMMM 'de' yyyy",
            Locale("es", "EC")
        ).format(selectedDate.time)
        dateView.text = formatted.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        try {
            val day = LiturgicalResolver.resolve(this, selectedDate)
            celebrationView.text = day.celebration
            var detail = day.liturgicalColor ?: ""
            val week = LiturgicalResolver.ordinaryWeekNumber(selectedDate)
            if (week > 0) {
                if (detail.isNotEmpty()) detail += " · "
                detail += "Semana $week del Tiempo Ordinario"
            }
            detailView.text = detail
        } catch (_: Exception) {
            celebrationView.text = "Celebración del día"
            detailView.text = "Calendario litúrgico"
        }
        renderSections()
    }

    private fun renderSections() {
        sections.removeAllViews()
        MissalV5Semantic.sections().forEach { section ->
            addSection(section.title, section.summary, section.id)
        }
    }

    private fun addSection(title: String, subtitle: String, id: String) {
        val card = column().apply {
            setPadding(dp(16), dp(15), dp(16), dp(15))
            setBackgroundResource(R.drawable.bg_button_secondary)
            addView(text(title, 19, R.color.ink, true))
            addView(text(subtitle, 14, R.color.muted, false).apply {
                setPadding(0, dp(4), 0, 0)
            })
            setOnClickListener { openSection(id, title) }
        }
        sections.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, dp(10))
        })
    }

    private fun openSection(id: String, title: String) {
        startActivity(Intent(this, MissalV5SectionActivity::class.java).apply {
            putExtra(MissalV5SectionActivity.EXTRA_SECTION, id)
            putExtra(MissalV5SectionActivity.EXTRA_TITLE, title)
            putExtra(MissalV5SectionActivity.EXTRA_YEAR, selectedDate.get(Calendar.YEAR))
            putExtra(MissalV5SectionActivity.EXTRA_MONTH, selectedDate.get(Calendar.MONTH))
            putExtra(MissalV5SectionActivity.EXTRA_DAY, selectedDate.get(Calendar.DAY_OF_MONTH))
            putExtra(MissalV5SectionActivity.EXTRA_CELEBRATION, celebrationView.text.toString())
        })
    }

    private fun chooseDate() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day, 12, 0, 0)
                refreshDay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun moveDay(amount: Int) {
        selectedDate.add(Calendar.DATE, amount)
        refreshDay()
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }

    private fun action(value: String) = text(value, 30, R.color.wine, false).apply {
        gravity = Gravity.CENTER
    }

    private fun text(value: String, sp: Int, colorRes: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = sp.toFloat()
        setTextColor(color(colorRes))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    @Suppress("DEPRECATION")
    private fun color(res: Int): Int = resources.getColor(res)

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)
}
