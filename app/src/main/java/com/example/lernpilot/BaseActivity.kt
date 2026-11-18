package com.example.lernpilot

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        private val worker = Executors.newSingleThreadExecutor()
    }

    protected val cyan = Color.rgb(81, 255, 232)
    protected val pink = Color.rgb(255, 82, 196)
    protected val lightText = Color.rgb(223, 250, 255)
    protected val mutedText = Color.rgb(150, 181, 198)
    private val panelColor = Color.rgb(12, 28, 42)

    protected val db: QuizRepository
        get() = QuizRepository.get(this)

    protected lateinit var content: LinearLayout

    protected fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    protected fun setup(layoutId: Int) {
        enableEdgeToEdge()
        setContentView(layoutId)

        content = findViewById(R.id.content)
        content.setPadding(dp(24), dp(24), dp(24), dp(32))

        val root = findViewById<View>(R.id.main)
        root.background = RetroBackground(resources.displayMetrics.density)

        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }
    }

    private fun panel(
        fill: Int,
        border: Int,
        radius: Int = 8
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), border)
        }
    }

    protected fun label(
        value: String,
        size: Float = 18f
    ): TextView {
        return TextView(this).also { text ->
            val isTitle = size >= 26f

            text.text = value
            text.textSize = size
            text.typeface = if (isTitle) {
                Typeface.create("monospace", Typeface.BOLD)
            } else {
                Typeface.MONOSPACE
            }

            text.setTextColor(
                when {
                    isTitle -> cyan
                    size >= 22f -> lightText
                    else -> mutedText
                }
            )

            text.setLineSpacing(dp(3).toFloat(), 1.05f)
            text.setPadding(0, dp(10), 0, dp(12))

            if (isTitle) {
                text.letterSpacing = 0.06f
                text.setShadowLayer(
                    dp(5).toFloat(),
                    0f,
                    0f,
                    Color.argb(90, 81, 255, 232)
                )
            }

            content.addView(text)
        }
    }

    protected fun button(
        value: String,
        action: () -> Unit
    ): Button {
        return Button(this).also { button ->
            button.text = value
            button.isAllCaps = true
            button.textSize = 15f
            button.typeface = Typeface.create(
                "monospace",
                Typeface.BOLD
            )
            button.letterSpacing = 0.06f
            button.gravity = Gravity.CENTER

            button.setTextColor(
                ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                    ),
                    intArrayOf(mutedText, cyan)
                )
            )

            button.backgroundTintList = null
            button.background = RippleDrawable(
                ColorStateList.valueOf(
                    Color.argb(70, 81, 255, 232)
                ),
                panel(panelColor, cyan),
                null
            )

            button.stateListAnimator = null
            button.minimumHeight = dp(56)
            button.setPadding(dp(16), dp(14), dp(16), dp(14))

            button.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(4)
            }

            content.addView(button)
            button.setOnClickListener { action() }
        }
    }

    protected fun styleAnswer(
        radio: RadioButton,
        selected: Boolean,
        submitted: Boolean,
        correct: Boolean
    ) {
        val border = when {
            submitted && correct -> cyan
            submitted && selected -> pink
            selected -> cyan
            else -> Color.rgb(48, 78, 95)
        }

        val fill = when {
            submitted && selected && !correct ->
                Color.rgb(58, 17, 46)

            selected || (submitted && correct) ->
                Color.rgb(13, 52, 58)

            else -> panelColor
        }

        radio.typeface = Typeface.MONOSPACE
        radio.textSize = 16f
        radio.setTextColor(lightText)
        radio.setPadding(dp(12), dp(14), dp(12), dp(14))
        radio.minHeight = dp(56)

        radio.backgroundTintList = null
        radio.background = panel(fill, border)

        radio.buttonTintList = ColorStateList.valueOf(border)

        radio.layoutParams = android.widget.RadioGroup.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(10)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            radio.stateDescription = when {
                submitted && correct -> "Correct answer"
                submitted && selected -> "Your answer, incorrect"
                else -> null
            }
        }
    }

    protected fun <T> load(
        work: () -> T,
        done: (T) -> Unit
    ) {
        worker.execute {
            val result = runCatching(work)

            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    result.fold(
                        onSuccess = done,
                        onFailure = { error ->
                            content.removeAllViews()
                            label("CONNECTION INTERRUPTED", 26f)
                            label("Unable to load data: ${error.message}")
                            button("Retry") { recreate() }
                        }
                    )
                }
            }
        }
    }
}