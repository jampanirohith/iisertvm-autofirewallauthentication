package com.example.iiserautologin

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
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
            setBackgroundColor(Color.rgb(13, 126, 216))
        }

        val backButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            setColorFilter(Color.WHITE)
            contentDescription = "Back"
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener { finish() }
        }
        topBar.addView(backButton, LinearLayout.LayoutParams(dp(56), dp(56)))

        val title = TextView(this).apply {
            text = "Settings"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(
            title,
            LinearLayout.LayoutParams(0, dp(56), 1f)
        )

        root.addView(topBar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        val heading = TextView(this).apply {
            text = "IISER Account"
            textSize = 24f
            setTextColor(Color.rgb(25, 25, 25))
        }
        content.addView(heading, marginParams(bottom = 8))

        val description = TextView(this).apply {
            text = "Enter the single email address and password that the app should use for IISER Wi-Fi authentication."
            textSize = 15f
            setTextColor(Color.rgb(85, 85, 85))
        }
        content.addView(description, marginParams(bottom = 22))

        val emailLabel = TextView(this).apply {
            text = "Email address"
            textSize = 14f
            setTextColor(Color.rgb(70, 70, 70))
        }
        content.addView(emailLabel, marginParams(bottom = 6))

        emailInput = EditText(this).apply {
            hint = "you@example.com"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        content.addView(emailInput, marginParams(bottom = 18))

        val passwordLabel = TextView(this).apply {
            text = "Password"
            textSize = 14f
            setTextColor(Color.rgb(70, 70, 70))
        }
        content.addView(passwordLabel, marginParams(bottom = 6))

        passwordInput = EditText(this).apply {
            hint = "Password"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        content.addView(passwordInput, marginParams(bottom = 6))

        val showPassword = CheckBox(this).apply {
            text = "Show password"
            setTextColor(Color.rgb(70, 70, 70))
            setOnCheckedChangeListener { _, checked ->
                passwordInput.inputType = if (checked) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                passwordInput.setSelection(passwordInput.text.length)
            }
        }
        content.addView(showPassword, marginParams(bottom = 20))

        val saveButton = Button(this).apply {
            text = "Save Account"
            setOnClickListener { saveCredentials() }
        }
        content.addView(saveButton, marginParams(bottom = 12))

        val clearButton = Button(this).apply {
            text = "Remove Saved Account"
            setOnClickListener {
                CredentialStore.clear(this@SettingsActivity)

                if (IISERAuthService.isRunning) {
                    stopService(
                        android.content.Intent(
                            this@SettingsActivity,
                            IISERAuthService::class.java
                        )
                    )
                }

                emailInput.text.clear()
                passwordInput.text.clear()

                Toast.makeText(
                    this@SettingsActivity,
                    "Saved account removed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        content.addView(clearButton, marginParams(bottom = 22))

        val privacyNote = TextView(this).apply {
            text = "Your account is stored locally on this device. The password is encrypted using Android Keystore before it is saved."
            textSize = 13f
            setTextColor(Color.rgb(95, 95, 95))
        }
        content.addView(privacyNote)

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
        loadExistingCredentials()
    }

    private fun loadExistingCredentials() {
        val credentials = CredentialStore.get(this) ?: return
        emailInput.setText(credentials.email)
        passwordInput.setText(credentials.password)
    }

    private fun saveCredentials() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isBlank()) {
            emailInput.error = "Enter your email address"
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Enter your password"
            passwordInput.requestFocus()
            return
        }

        try {
            CredentialStore.save(this, email, password)

            // If the service is already running, tell it to retry immediately with
            // the newly saved credentials.
            if (IISERAuthService.isRunning) {
                startService(
                    android.content.Intent(this, IISERAuthService::class.java).apply {
                        action = IISERAuthService.ACTION_AUTHENTICATE_NOW
                    }
                )
            }

            Toast.makeText(
                this,
                "Account saved.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not save the account.",
                Toast.LENGTH_LONG
            ).show()
        }
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
