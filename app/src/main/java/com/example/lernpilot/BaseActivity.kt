package com.example.lernpilot

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        // Shared queue: database operations execute one at a time.
        private val worker = Executors.newSingleThreadExecutor()
    }

    protected val db: DatabaseHelper
        get() = DatabaseHelper.get(this)

    protected lateinit var content: LinearLayout

    protected fun setup(layoutId: Int) {
        enableEdgeToEdge()
        setContentView(layoutId)

        content = findViewById(R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                bars.left, bars.top, bars.right, bars.bottom
            )

            insets
        }
    }

    protected fun label(
        value: String,
        size: Float = 18f
    ): TextView {
        return TextView(this).also {
            it.text = value
            it.textSize = size
            val spacing = (12 * resources.displayMetrics.density).toInt()
            it.setPadding(0, spacing, 0, spacing)
            content.addView(it)
        }
    }

    protected fun button(
        value: String,
        action: () -> Unit
    ): Button {
        return Button(this).also {
            it.text = value
            content.addView(it)
            it.setOnClickListener { action() }
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
                            label("Unable to load data: ${error.message}")
                            button("Retry") { recreate() }
                        }
                    )
                }
            }
        }
    }
}