package com.example.iiserautologin

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var accountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) {
            refreshUi()
        }
    }

    private fun buildUi() {
        window.statusBarColor = Color.rgb(9, 83, 148)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(6), 0)
            setBackgroundColor(Color.rgb(13, 126, 216))
        }

        val title = TextView(this).apply {
            text = "IISER Auto Login"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }

        val settingsButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Color.WHITE)
            contentDescription = "Settings"
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        topBar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(56), 1f)
        )
        topBar.addView(
            settingsButton,
            LinearLayout.LayoutParams(dp(56), dp(56))
        )
        root.addView(topBar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        val heading = TextView(this).apply {
            text = "Automatic IISER Wi-Fi Login"
            textSize = 24f
            setTextColor(Color.rgb(25, 25, 25))
        }
        content.addView(heading, marginParams(bottom = 8))

        val description = TextView(this).apply {
            text = "Connect to the IISER Wi-Fi and let the app handle gateway authentication automatically."
            textSize = 16f
            setTextColor(Color.rgb(85, 85, 85))
        }
        content.addView(description, marginParams(bottom = 24))

        statusText = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.rgb(30, 30, 30))
        }
        content.addView(statusText, marginParams(bottom = 8))

        accountText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.rgb(85, 85, 85))
        }
        content.addView(accountText, marginParams(bottom = 24))

        val startButton = Button(this).apply {
            text = "Start Auto Login"
            setOnClickListener { startAutoLogin() }
        }
        content.addView(
            startButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        )

        val stopButton = Button(this).apply {
            text = "Stop Auto Login"
            setOnClickListener { stopAutoLogin() }
        }
        content.addView(
            stopButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        )

        val settingsHint = TextView(this).apply {
            text = "Need to change the account? Tap the ⚙ Settings icon above."
            textSize = 14f
            setTextColor(Color.rgb(95, 95, 95))
        }
        content.addView(settingsHint)

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        refreshUi()
    }

    private fun refreshUi() {
        val credentials = CredentialStore.get(this)

        statusText.text = if (IISERAuthService.isRunning) {
            "Status: Auto Login is running"
        } else {
            "Status: Auto Login is stopped"
        }

        accountText.text = if (credentials != null) {
            "Account: ${credentials.email}"
        } else {
            "Account: Not configured — open Settings to add one"
        }
    }

    private fun startAutoLogin() {
        if (!CredentialStore.hasCredentials(this)) {
            Toast.makeText(
                this,
                "Add your IISER email and password in Settings first.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, IISERAuthService::class.java)
        )

        statusText.text = "Status: Auto Login is running"
    }

    private fun stopAutoLogin() {
        stopService(Intent(this, IISERAuthService::class.java))
        statusText.text = "Status: Auto Login is stopped"
    }

    private fun marginParams(
        top: Int = 0,
        bottom: Int = 0,
        left: Int = 0,
        right: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            this.topMargin = dp(top)
            this.bottomMargin = dp(bottom)
            this.leftMargin = dp(left)
            this.rightMargin = dp(right)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
