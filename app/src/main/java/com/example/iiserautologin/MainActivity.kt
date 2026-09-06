package com.example.iiserautologin

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val statusText = TextView(this).apply {
            text = "IISER Auto Login"
            textSize = 20f
            setPadding(32, 32, 32, 32)
        }

        val startButton = Button(this).apply {
            text = "Start Auto Login"
        }

        val stopButton = Button(this).apply {
            text = "Stop Auto Login"
        }

        startButton.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }

            val intent =
                Intent(this, IISERAuthService::class.java)

            ContextCompat.startForegroundService(
                this,
                intent
            )

            statusText.text =
                "Auto Login is running"
        }

        stopButton.setOnClickListener {

            stopService(
                Intent(
                    this,
                    IISERAuthService::class.java
                )
            )

            statusText.text =
                "Auto Login stopped"
        }

        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)

                addView(
                    statusText,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                addView(
                    startButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                addView(
                    stopButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

        setContentView(layout)
    }
}