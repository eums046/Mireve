package com.example.mireve

data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    val content: String? = null,
    val checklist: List<String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)