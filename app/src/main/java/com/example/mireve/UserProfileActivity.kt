package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

class UserProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_user_profile)
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("UserProfileActivity", "User: $user")
            val emailTextView = findViewById<TextView>(R.id.tvUserEmail)
            emailTextView.text = user?.email ?: "No email found"
            findViewById<Button>(R.id.btnLogout).setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("UserProfileActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading profile screen", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("UserProfileActivity", "onStart user: $user")
            if (user == null) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("UserProfileActivity", "Error in onStart", e)
            Toast.makeText(this, "Error loading profile screen", Toast.LENGTH_LONG).show()
            finish()
        }
    }
} 