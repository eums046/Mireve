package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.etConfirmPassword)
        val signupButton = findViewById<Button>(R.id.btnSignup)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, DiaryListActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Set underlined and clickable 'Login'
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
}