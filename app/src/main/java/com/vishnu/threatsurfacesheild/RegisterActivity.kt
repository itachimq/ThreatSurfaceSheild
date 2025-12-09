package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RegisterActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authManager = AuthManager(this)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        statusText = findViewById(R.id.registerStatusText)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val backToLoginButton = findViewById<TextView>(R.id.backToLoginButton)

        createAccountButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            when (val result = authManager.registerUser(email, password)) {
                is AuthResult.Success -> {
                    updateStatus("Registration successful! You can now log in.", false)
                    // Navigate back to login after a short delay
                    emailInput.postDelayed({
                        finish() // Go back to AuthActivity
                    }, 1500)
                }
                is AuthResult.Failure -> {
                    updateStatus(result.message, true)
                }
            }
        }

        backToLoginButton.setOnClickListener {
            finish() // Simply close this screen to go back to the login page
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
