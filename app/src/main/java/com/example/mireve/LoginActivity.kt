package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import android.os.Handler
import android.os.Looper

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        val signupLink = findViewById<TextView>(R.id.tvSignupLink)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        progressBar.visibility = View.GONE

        loginButton.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            val email = SecurityManager.sanitizeInput(emailEditText.text.toString())
            val password = passwordEditText.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }

            if (!SecurityManager.isValidEmail(email)) {
                showError("Please enter a valid email address")
                return@setOnClickListener
            }

            if (SecurityManager.isAccountLocked(this, email)) {
                val remainingTime = SecurityManager.getRemainingLockoutTime(this, email)
                showError("Account is temporarily locked due to too many failed attempts. Try again in $remainingTime minutes.")
                return@setOnClickListener
            }

            startLoginProcess(email, password)
        }

        val fullText = getString(R.string.signup_link).replace("<u>", "").replace("</u>", "")
        val signUpText = "Sign up"
        val start = fullText.indexOf(signUpText)
        val end = start + signUpText.length
        val spannable = android.text.SpannableString(fullText)
        spannable.setSpan(android.text.style.UnderlineSpan(), start, end, 0)
        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: android.view.View) {
                startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
            }
        }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        signupLink.text = spannable
        signupLink.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        forgotPassword.setOnClickListener {
            val email = SecurityManager.sanitizeInput(emailEditText.text.toString())
            if (email.isEmpty()) {
                showError("Enter your email to reset password")
            } else if (!SecurityManager.isValidEmail(email)) {
                showError("Please enter a valid email address")
            } else {
                startPasswordResetProcess(email)
            }
        }
    }

    private fun startLoginProcess(email: String, password: String) {
        isProcessing = true
        showLoading(true)
        
        SecurityManager.logSecurityEvent("Login Attempt", "Email: $email")
        
        Handler(Looper.getMainLooper()).postDelayed({
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isProcessing = false
                    showLoading(false)
                    
                    if (task.isSuccessful) {
                        SecurityManager.logSecurityEvent("Login Success", "Email: $email")
                        
                        SecurityManager.resetFailedLoginAttempts(this, email)
                        
                        LoginManager.refreshLoginState(this, email)
                        
                        val deviceId = SecurityManager.getDeviceId(this)
                        SecurityManager.logSecurityEvent("Login Device", "Device: $deviceId")
                        
                        val intent = Intent(this, DiaryListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        SecurityManager.logSecurityEvent("Login Failed", "Email: $email, Error: $errorMessage")
                        
                        SecurityManager.recordFailedLoginAttempt(this, email)
                        
                        val userFriendlyMessage = when {
                            errorMessage.contains("password is invalid") -> 
                                "Invalid email or password. Please try again."
                            errorMessage.contains("no user record") -> 
                                "No account found with this email. Please check your email or sign up."
                            errorMessage.contains("network") -> 
                                "Network error. Please check your internet connection and try again."
                            errorMessage.contains("too many requests") -> 
                                "Too many login attempts. Please try again later."
                            else -> "Login failed: $errorMessage"
                        }
                        
                        showError(userFriendlyMessage)
                        
                        if (SecurityManager.isAccountLocked(this, email)) {
                            val remainingTime = SecurityManager.getRemainingLockoutTime(this, email)
                            showError("Account locked due to too many failed attempts. Try again in $remainingTime minutes.")
                        }
                    }
                }
        }, 1000)
    }

    private fun startPasswordResetProcess(email: String) {
        SecurityManager.logSecurityEvent("Password Reset Attempt", "Email: $email")
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    SecurityManager.logSecurityEvent("Password Reset Email Sent", "Email: $email")
                    showSuccess("Password reset email sent to $email")
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    SecurityManager.logSecurityEvent("Password Reset Failed", "Email: $email, Error: $errorMessage")
                    
                    val userFriendlyMessage = when {
                        errorMessage.contains("no user record") -> 
                            "No account found with this email address."
                        errorMessage.contains("network") -> 
                            "Network error. Please check your internet connection and try again."
                        else -> "Error sending password reset email: $errorMessage"
                    }
                    
                    showError(userFriendlyMessage)
                }
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        loginButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}