package com.example.mireve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import android.util.Log
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "Firebase initialized: "+ (FirebaseApp.getInstance() != null))
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginState()
        }, 1500)
    }
    
    private fun checkLoginState() {
        val isLoggedIn = LoginManager.isLoggedIn(this)
        val isSessionValid = LoginManager.isSessionValid(this)
        val user = auth.currentUser
        
        Log.d("MainActivity", "Login check - Local login: $isLoggedIn, Session valid: $isSessionValid, Firebase user: ${user?.email}")
        
        if (isLoggedIn && isSessionValid) {
            if (user != null) {
                Log.d("MainActivity", "User is logged in: ${user.email}")
                startActivity(Intent(this, DiaryListActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity", "Local login found but Firebase session unavailable, attempting to restore...")
                attemptSessionRestore()
            }
        } else {
            Log.d("MainActivity", "User is not logged in or session expired")
            LoginManager.clearLoginState(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun attemptSessionRestore() {
        Handler(Looper.getMainLooper()).postDelayed({
            val user = auth.currentUser
            if (user != null) {
                Log.d("MainActivity", "Firebase session restored for user: ${user.email}")
                startActivity(Intent(this, DiaryListActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity", "Firebase session couldn't be restored, navigating to app with local state")
                startActivity(Intent(this, DiaryListActivity::class.java))
                finish()
            }
        }, 1000)
    }
}