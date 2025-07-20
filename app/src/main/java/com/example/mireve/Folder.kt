package com.example.mireve

data class Folder(
    val id: String = "",
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
) 