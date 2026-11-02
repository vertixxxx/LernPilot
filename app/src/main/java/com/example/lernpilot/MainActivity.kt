package com.example.lernpilot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the dashboard XML layout.
        setContentView(R.layout.activity_main)

        // Find the button defined in that layout.
        val startButton = findViewById<Button>(R.id.startLearningButton)

        // Open TopicsActivity when the button is pressed.
        startButton.setOnClickListener {
            val intent = Intent(this, TopicsActivity::class.java)
            startActivity(intent)
        }
    }
}