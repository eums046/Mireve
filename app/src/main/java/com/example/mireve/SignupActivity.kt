package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import android.os.Handler
import android.os.Looper

class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var signupButton: Button
    private lateinit var progressBar: ProgressBar
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.etConfirmPassword)
        signupButton = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        progressBar.visibility = View.GONE

        signupButton.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            
            val email = SecurityManager.sanitizeInput(emailEditText.text.toString())
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            val validationResult = SecurityManager.validateSignupData(email, password, confirmPassword)
            if (!validationResult.isValid) {
                showError(validationResult.errorMessage ?: "Invalid input")
                return@setOnClickListener
            }

            if (SecurityManager.isAccountLocked(this, email)) {
                val remainingTime = SecurityManager.getRemainingLockoutTime(this, email)
                showError("Account is temporarily locked. Try again in $remainingTime minutes.")
                return@setOnClickListener
            }

            startSignupProcess(email, password)
        }

        val fullText = getString(R.string.back_to_login).replace("<u>", "").replace("</u>", "")
        val loginText = "Login"
        val start = fullText.indexOf(loginText)
        val end = start + loginText.length
        val spannable = android.text.SpannableString(fullText)
        spannable.setSpan(android.text.style.UnderlineSpan(), start, end, 0)
        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: android.view.View) {
                finish()
            }
        }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        backToLogin.text = spannable
        backToLogin.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun startSignupProcess(email: String, password: String) {
        isProcessing = true
        showLoading(true)
        
        SecurityManager.logSecurityEvent("Signup Attempt", "Email: $email")
        
        Handler(Looper.getMainLooper()).postDelayed({
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    isProcessing = false
                    showLoading(false)
                    
                    if (task.isSuccessful) {
                        SecurityManager.logSecurityEvent("Signup Success", "Email: $email")
                        
                        SecurityManager.resetFailedLoginAttempts(this, email)
                        
                        LoginManager.refreshLoginState(this, email)
                        
                        val deviceId = SecurityManager.getDeviceId(this)
                        SecurityManager.logSecurityEvent("Device Registration", "Device: $deviceId")
                        
                        val intent = Intent(this, DiaryListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        SecurityManager.logSecurityEvent("Signup Failed", "Email: $email, Error: $errorMessage")
                        
                        val userFriendlyMessage = when {
                            errorMessage.contains("email address is already in use") -> 
                                "An account with this email already exists. Please try logging in instead."
                            errorMessage.contains("network") -> 
                                "Network error. Please check your internet connection and try again."
                            errorMessage.contains("invalid") -> 
                                "Invalid email or password format. Please check your input."
                            else -> "Signup failed: $errorMessage"
                        }
                        
                        showError(userFriendlyMessage)
                    }
                }
        }, 1000)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        signupButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}