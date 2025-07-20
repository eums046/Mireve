package com.example.mireve

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddEditDiaryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var entryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_diary)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val titleEditText = findViewById<EditText>(R.id.etTitle)
        val contentEditText = findViewById<EditText>(R.id.etContent)
        val saveButton = findViewById<Button>(R.id.btnSave)

        entryId = intent.getStringExtra("ENTRY_ID")

        if (entryId != null) {
            // Editing existing entry
            db.collection("users").document(userId).collection("entries").document(entryId!!)
                .get()
                .addOnSuccessListener { document ->
                    val entry = document.toObject(DiaryEntry::class.java)
                    if (entry != null) {
                        titleEditText.setText(entry.title)
                        contentEditText.setText(entry.content)
                    }
                }
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val content = contentEditText.text.toString().trim()
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val id = entryId ?: db.collection("users").document(userId).collection("entries").document().id
            val entry = DiaryEntry(
                id = id,
                title = title,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            db.collection("users").document(userId).collection("entries").document(id)
                .set(entry)
                .addOnSuccessListener {
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show()
                }
        }
    }
}