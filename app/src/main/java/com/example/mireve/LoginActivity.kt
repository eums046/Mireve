package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupLink = findViewById<TextView>(R.id.tvSignupLink)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, DiaryListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Set underlined and clickable 'Sign up'
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
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}