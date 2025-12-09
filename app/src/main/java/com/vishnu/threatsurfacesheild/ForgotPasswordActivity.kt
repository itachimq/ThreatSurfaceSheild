package com.vishnu.threatsurfaceshield

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var emailInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        authManager = AuthManager(this)

        emailInput = findViewById(R.id.emailInput_forgot)
        statusText = findViewById(R.id.resetStatusText)
        val sendResetButton = findViewById<Button>(R.id.sendResetButton)
        val backToLoginButton = findViewById<TextView>(R.id.backToLoginButton_forgot)

        sendResetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                updateStatus("Please enter your email address.", true)
                return@setOnClickListener
            }

            when (val result = authManager.resetPassword(email)) {
                is AuthResult.Success -> {
                    updateStatus("If an account with this email exists, it has been reset. You can now sign up again.", false)
                }
                is AuthResult.Failure -> {
                    // Show the same success message to prevent user enumeration
                    updateStatus("If an account with this email exists, it has been reset. You can now sign up again.", false)
                }
            }
        }

        backToLoginButton.setOnClickListener {
            finish() // Go back to the login screen
        }
    }

    private fun updateStatus(message: String, isError: Boolean) {
        statusText.text = message
        statusText.setTextColor(
            if (isError) ContextCompat.getColor(this, R.color.red)
            else ContextCompat.getColor(this, R.color.green)
        )
    }
}
