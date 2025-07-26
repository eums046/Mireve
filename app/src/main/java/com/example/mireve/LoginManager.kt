package com.example.mireve

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object LoginManager {
    private const val PREFS_NAME = "MirevePrefs"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_USER_EMAIL = "userEmail"
    private const val KEY_LOGIN_TIMESTAMP = "loginTimestamp"
    
    fun isLoggedIn(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clearLoginState(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_LOGIN_TIMESTAMP)
        editor.apply()
        Log.d("LoginManager", "Login state cleared")
    }
    
    fun isSessionValid(context: Context): Boolean {
        if (!isLoggedIn(context)) return false
        
        val loginTimestamp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LOGIN_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        val sessionTimeout = 30 * 24 * 60 * 60 * 1000L
        
        if (currentTime - loginTimestamp > sessionTimeout) {
            Log.d("LoginManager", "Session expired, clearing login state")
            clearLoginState(context)
            return false
        }
        
        return true
    }
    
    fun shouldAttemptSessionRestore(context: Context): Boolean {
        return isLoggedIn(context) && isSessionValid(context)
    }
    
    fun refreshLoginState(context: Context, email: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
        Log.d("LoginManager", "Login state refreshed for user: $email")
    }
} 