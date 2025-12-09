package com.vishnu.threatsurfaceshield

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun registerUser(email: String, password: String): AuthResult {
        if (!isEmailValid(email)) {
            return AuthResult.Failure("Invalid email format.")
        }
        if (password.length < 6) {
            return AuthResult.Failure("Password must be at least 6 characters long.")
        }
        if (prefs.contains(email)) {
            return AuthResult.Failure("User with this email already exists.")
        }

        val hashedPassword = hashPassword(password)
        prefs.edit().putString(email, hashedPassword).apply()
        return AuthResult.Success
    }

    fun loginUser(email: String, password: String): AuthResult {
        if (!prefs.contains(email)) {
            return AuthResult.Failure("User not found. Please sign up.")
        }

        val storedPasswordHash = prefs.getString(email, null)
        val inputPasswordHash = hashPassword(password)

        if (storedPasswordHash != inputPasswordHash) {
            return AuthResult.Failure("Incorrect password.")
        }

        // Set the current logged-in user
        prefs.edit().putString("current_user", email).apply()
        return AuthResult.Success
    }

    fun resetPassword(email: String): AuthResult {
        if (!prefs.contains(email)) {
            return AuthResult.Failure("No account found for this email.")
        }
        // This removes the user's password data, allowing them to register again.
        prefs.edit().remove(email).apply()
        return AuthResult.Success
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.contains("current_user")
    }

    fun logoutUser() {
        prefs.edit().remove("current_user").apply()
    }

    fun clearAllUsers() {
        prefs.edit().clear().apply()
    }

    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Failure(val message: String) : AuthResult()
}
