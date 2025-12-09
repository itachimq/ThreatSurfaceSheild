package com.vishnu.threatsurfaceshield

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AuthActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager(this)

        // --- Smart Router Logic ---
        val intentAction = intent.action
        val intentData = intent.data

        if (intentAction == Intent.ACTION_VIEW && intentData != null) {
            // Launched by clicking a link. Bypass auth and go to browser.
            val browserIntent = Intent(this, SecureBrowserActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = intentData
            }
            startActivity(browserIntent)
            finish()
            return // Skip the rest of the AuthActivity logic
        }

        // --- Normal App Launch Logic ---
        if (authManager.isUserLoggedIn()) {
            // User is already logged in, go straight to the dashboard.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // If not logged in and not a link, show the login screen.
        setContentView(R.layout.activity_auth)
        setupAuthView()
    }

    private fun setupAuthView() {
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<TextView>(R.id.signUpButton)
        val forgotPasswordButton = findViewById<TextView>(R.id.forgotPasswordButton)
        val authStatusText = findViewById<TextView>(R.id.authStatusText)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            handleLogin(email, password, authStatusText)
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun handleLogin(email: String, pass: String, statusView: TextView) {
        if (email.isEmpty() || pass.isEmpty()) {
            updateStatus(statusView, "Email and password cannot be empty.", true)
            return
        }

        when (val result = authManager.loginUser(email, pass)) {
            is AuthResult.Success -> {
                updateStatus(statusView, "Login successful!", false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            is AuthResult.Failure -> {
                updateStatus(statusView, result.message, true)
            }
        }
    }

    private fun updateStatus(view: TextView, message: String, isError: Boolean) {
        view.text = message
        view.setTextColor(
            if (isError) ContextCompat.getColor(this, R.color.red)
            else ContextCompat.getColor(this, R.color.green)
        )
    }
}
