package com.example.mireve

import android.content.Context
import android.util.Log
import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.regex.Pattern
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

object SecurityManager {
    private const val TAG = "SecurityManager"
    private const val PREFS_NAME = "SecurityPrefs"
    private const val KEY_LOGIN_ATTEMPTS = "login_attempts"
    private const val KEY_LAST_LOGIN_ATTEMPT = "last_login_attempt"
    private const val KEY_ACCOUNT_LOCKOUT_UNTIL = "account_lockout_until"
    private const val KEY_DEVICE_ID = "device_id"
    
    private const val MAX_LOGIN_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MINUTES = 15
    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PASSWORD_LENGTH = 128
    
    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    )
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun validatePassword(password: String): PasswordValidationResult {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "Password must be at least $MIN_PASSWORD_LENGTH characters long"
            )
        }
        
        if (password.length > MAX_PASSWORD_LENGTH) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "Password must be less than $MAX_PASSWORD_LENGTH characters"
            )
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@#$%^&+=!)"
            )
        }
        
        if (isCommonPassword(password)) {
            return PasswordValidationResult(
                isValid = false,
                errorMessage = "This password is too common. Please choose a stronger password"
            )
        }
        
        return PasswordValidationResult(isValid = true, errorMessage = null)
    }
    
    fun isAccountLocked(context: Context, email: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong("${KEY_ACCOUNT_LOCKOUT_UNTIL}_$email", 0)
        return System.currentTimeMillis() < lockoutUntil
    }
    
    fun getRemainingLockoutTime(context: Context, email: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong("${KEY_ACCOUNT_LOCKOUT_UNTIL}_$email", 0)
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) TimeUnit.MILLISECONDS.toMinutes(remaining) else 0
    }
    
    fun recordFailedLoginAttempt(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attempts = prefs.getInt("${KEY_LOGIN_ATTEMPTS}_$email", 0) + 1
        val editor = prefs.edit()
        
        editor.putInt("${KEY_LOGIN_ATTEMPTS}_$email", attempts)
        editor.putLong("${KEY_LAST_LOGIN_ATTEMPT}_$email", System.currentTimeMillis())
        
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            val lockoutUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION_MINUTES.toLong())
            editor.putLong("${KEY_ACCOUNT_LOCKOUT_UNTIL}_$email", lockoutUntil)
            Log.w(TAG, "Account locked for $email due to $attempts failed attempts")
        }
        
        editor.apply()
    }
    
    fun resetFailedLoginAttempts(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("${KEY_LOGIN_ATTEMPTS}_$email")
        editor.remove("${KEY_LAST_LOGIN_ATTEMPT}_$email")
        editor.remove("${KEY_ACCOUNT_LOCKOUT_UNTIL}_$email")
        editor.apply()
        Log.d("LoginManager", "Reset failed login attempts for $email")
    }
    
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            deviceId = generateSecureDeviceId()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }
    
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("[<>\"']"), "")
            .replace(Regex("\\s+"), " ")
    }
    
    fun validateSignupData(email: String, password: String, confirmPassword: String): SignupValidationResult {
        if (!isValidEmail(email)) {
            return SignupValidationResult(
                isValid = false,
                errorMessage = "Please enter a valid email address"
            )
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            return SignupValidationResult(
                isValid = false,
                errorMessage = passwordValidation.errorMessage
            )
        }
        
        if (password != confirmPassword) {
            return SignupValidationResult(
                isValid = false,
                errorMessage = "Passwords do not match"
            )
        }
        
        return SignupValidationResult(isValid = true, errorMessage = null)
    }
    
    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "123456", "123456789", "qwerty", "abc123", "password123",
            "admin", "letmein", "welcome", "monkey", "12345678", "1234567",
            "sunshine", "master", "hello", "freedom", "whatever", "qazwsx",
            "trustno1", "dragon", "baseball", "superman", "iloveyou", "starwars"
        )
        return commonPasswords.contains(password.lowercase())
    }
    
    private fun generateSecureDeviceId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun logSecurityEvent(event: String, details: String? = null) {
        Log.i(TAG, "Security Event: $event${details?.let { " - $it" } ?: ""}")
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)

data class SignupValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
) 