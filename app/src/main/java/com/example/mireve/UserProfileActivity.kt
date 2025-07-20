package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class UserProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val user = FirebaseAuth.getInstance().currentUser
        val emailTextView = findViewById<TextView>(R.id.tvUserEmail)
        emailTextView.text = user?.email ?: "No email found"

        findViewById<Button>(R.id.btnLogoutProfile).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
} 