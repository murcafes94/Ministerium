package com.fabri.ministerium

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Native reader for Ministerium 5 Liturgia de las Horas. No WebView. */
class HoursV5ReaderActivity : ThemedActivity() {

    companion object {
        const val EXTRA_VOLUME_ID = "v5_hours_reader_volume"
        const val EXTRA_FILE_PATH = "v5_hours_reader_file"
        const val EXTRA_FRAGMENT = "v5_hours_reader_fragment"
        const val EXTRA_TITLE = "v5_hours_reader_title"
        const val EXTRA_SUBTITLE = "v5_hours_reader_subtitle"
        const val EXTRA_SCROLL_TEXT = "v5_hours_reader_scroll"
        const val EXTRA_HOUR_KEY = "v5_hours_reader_hour_key"
        const val EXTRA_OFFICE_RANK = "v5_hours_reader_office_rank"
        const val EXTRA_TEMPORAL_VOLUME_ID = "v5_hours_reader_temporal_volume"
        const val EXTRA_TEMPORAL_FILE_PATH = "v5_hours_reader_temporal_file"
        const val EXTRA_TEMPORAL_FRAGMENT = "v5_hours_reader_temporal_fragment"
        const val EXTRA_TEMPORAL_SCROLL_TEXT = "v5_hours_reader_temporal_scroll"
        const val EXTRA_COMMON_VOLUME_ID = "v5_hours_reader_common_volume"
        const val EXTRA_COMMON_FILE_PATH = "v5_hours_reader_common_file"
        const val EXTRA_COMMON_FRAGMENT = "v5_hours_reader_common_fragment"
        const val EXTRA_COMMON_TITLE = "v5_hours_reader_common_title"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var content: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        loadDocument()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildScreen(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.cream))
        }

        val root = column().apply {
            setPadding(dp(22), dp(18), dp(22), dp(34))
        }
        scroll.addView(root)

        val title = value(EXTRA_TITLE, "Liturgia de las Horas")
        root.addView(text("‹  $title", 23, R.color.wine, true).apply {
            setPadding(0, dp(6), 0, dp(5))
            setOnClickListener { finish() }
        })

        val subtitle = value(EXTRA_SUBTITLE, "")
        if (subtitle.isNotEmpty()) {
            root.addView(text(subtitle, 13, R.color.muted, false).apply {
                setPadding(0, 0, 0, dp(12))
            })
        }

        status = text("Preparando el oficio…", 12, R.color.muted, false).apply {
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(status)

        progress = ProgressBar(this)
        root.addView(progress, LinearLayout.LayoutParams(dp(32), dp(32)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setMargins(0, dp(6), 0, dp(16))
        })

        content = column()
        root.addView(content)
        return scroll
    }

    private fun loadDocument() {
        val volumeId = value(EXTRA_VOLUME_ID, "")
        val filePath = value(EXTRA_FILE_PATH, "")
        val fragment = value(EXTRA_FRAGMENT, "")
        val title = value(EXTRA_TITLE, "Liturgia de las Horas")
        val scrollText = value(EXTRA_SCROLL_TEXT, "")
        val hourKey = value(EXTRA_HOUR_KEY, "")
        val rank = value(EXTRA_OFFICE_RANK, "")
        val temporalVolume = value(EXTRA_TEMPORAL_VOLUME_ID, "")
        val temporalFile = value(EXTRA_TEMPORAL_FILE_PATH, "")
        val temporalFragment = value(EXTRA_TEMPORAL_FRAGMENT, "")
        val temporalScroll = value(EXTRA_TEMPORAL_SCROLL_TEXT, "")
        val commonVolume = value(EXTRA_COMMON_VOLUME_ID, "")
        val commonFile = value(EXTRA_COMMON_FILE_PATH, "")
        val commonFragment = value(EXTRA_COMMON_FRAGMENT, "")
        val commonTitle = value(EXTRA_COMMON_TITLE, "Común")

        executor.submit {
            try {
                val primary = loadNativeDocument(volumeId, filePath, fragment, title, scrollText)
                var document = primary
                var composed = false

                val temporal = if (temporalVolume.isNotEmpty() && temporalFile.isNotEmpty()) {
                    loadNativeDocument(temporalVolume, temporalFile, temporalFragment, title, temporalScroll)
                } else null

                val common = if (commonVolume.isNotEmpty() && commonFile.isNotEmpty()) {
                    loadNativeDocument(commonVolume, commonFile, commonFragment, commonTitle, "")
                } else null

                if (rank.isNotEmpty() && (temporal != null || common != null)) {
                    document = HoursV5Composition.compose(hourKey, rank, temporal, primary, common)
                    composed = true
                }

                val finalDocument = document
                val finalComposed = composed
                val usedCommon = common != null
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    progress.visibility = View.GONE
                    if (finalDocument.blocks.isEmpty()) {
                        status.text = "No se encontró contenido estructurado para este oficio."
                        addEmptyState()
                        return@runOnUiThread
                    }
                    status.text = when {
                        finalComposed && usedCommon -> "Texto local · temporal/propio/común con procedencia explícita"
                        finalComposed -> "Texto local · composición litúrgica semántica controlada"
                        else -> "Texto local · lector nativo"
                    }
                    render(finalDocument)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    progress.visibility = View.GONE
                    status.text = "No se pudo abrir este oficio sin conexión."
                    roleBlock(
                        "ESTADO",
                        "Contenido no disponible",
                        "Ministerium no sustituirá este oficio con contenido de otra celebración."
                    )
                }
            }
        }
    }

    private fun loadNativeDocument(
        volumeId: String,
        filePath: String,
        fragment: String,
        title: String,
        scrollText: String
    ): HoursNativeDocument {
        val volume = HoursRepository.find(volumeId)
            ?: throw IllegalStateException("Volumen del oficio no disponible.")
        if (filePath.isEmpty()) throw IllegalStateException("Ruta del oficio no disponible.")

        val root = EpubUtils.ensureExtracted(applicationContext, volume)
        val file = File(root, filePath)
        if (!file.isFile) throw IllegalStateException("El texto del oficio no está instalado.")
        val html = read(file)
        return HoursV5DocumentParser.parse(title, html, fragment, scrollText)
    }

    private fun render(document: HoursNativeDocument) {
        content.removeAllViews()
        document.blocks.forEach { block ->
            val role = role(block.type)
            if (block.title.isEmpty() && block.body.isEmpty()) return@forEach
            roleBlock(role, block.title, block.body)
        }
    }

    private fun role(type: HoursBlockType): String = when (type) {
        HoursBlockType.HYMN -> "HIMNO"
        HoursBlockType.ANTIPHON -> "ANTÍFONA"
        HoursBlockType.GOSPEL_ANTIPHON -> "ANTÍFONA EVANGÉLICA"
        HoursBlockType.PSALMODY -> "SALMODIA"
        HoursBlockType.FIRST_READING -> "PRIMERA LECTURA"
        HoursBlockType.SECOND_READING -> "SEGUNDA LECTURA"
        HoursBlockType.READING -> "LECTURA"
        HoursBlockType.RESPONSORY -> "RESPONSORIO"
        HoursBlockType.CANTICLE -> "CÁNTICO"
        HoursBlockType.INTERCESSIONS -> "PRECES"
        HoursBlockType.PRAYER -> "ORACIÓN"
        HoursBlockType.HEADING -> "SECCIÓN"
        HoursBlockType.TEXT -> "TEXTO"
    }

    private fun roleBlock(role: String, title: String, body: String) {
        val card = column().apply {
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_button_secondary)
        }

        card.addView(text(role, 10, R.color.muted, true).apply {
            letterSpacing = .10f
        })

        if (title.trim().isNotEmpty()) {
            card.addView(text(title.trim(), 18, R.color.wine, true).apply {
                setPadding(0, dp(3), 0, 0)
            })
        }

        if (body.trim().isNotEmpty()) {
            card.addView(text(body.trim(), 16, R.color.ink, false).apply {
                setPadding(0, dp(7), 0, 0)
                setLineSpacing(0f, 1.18f)
                setTextIsSelectable(true)
            })
        }

        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, dp(10))
        })
    }

    private fun addEmptyState() {
        roleBlock(
            "ESTADO",
            "Oficio sin contenido legible",
            "La estructura se mantuvo aislada para evitar mezclar temporal, santoral o comunes incorrectos."
        )
    }

    private fun read(file: File): String {
        FileInputStream(file).use { input: InputStream ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                return String(output.toByteArray(), StandardCharsets.UTF_8)
            }
        }
    }

    private fun value(key: String, fallback: String): String {
        val raw = intent.getStringExtra(key)?.trim().orEmpty()
        return raw.ifEmpty { fallback }
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2)
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
