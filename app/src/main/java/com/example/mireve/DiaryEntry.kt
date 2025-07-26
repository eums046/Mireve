package com.example.mireve

data class DiaryEntry(
    val id: String = "",
    val title: String = "",
    val content: String? = null,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem>? = null,
    val folderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val text: String = "",
    val isCompleted: Boolean = false
)