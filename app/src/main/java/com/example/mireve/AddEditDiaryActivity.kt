package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddEditDiaryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var entryId: String? = null
    private var isChecklistMode = false
    private val checklistEditTexts = mutableListOf<EditText>()
    private var folderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_diary)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val titleEditText = findViewById<EditText>(R.id.etTitle)
        val contentEditText = findViewById<EditText>(R.id.etContent)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val deleteButton = findViewById<Button>(R.id.btnDelete)
        val switchChecklist = findViewById<Switch>(R.id.switchChecklist)
        val checklistContainer = findViewById<LinearLayout>(R.id.checklistContainer)
        val btnAddChecklistItem = findViewById<Button>(R.id.btnAddChecklistItem)
        val tilContent = findViewById<View>(R.id.tilContent)

        entryId = intent.getStringExtra("ENTRY_ID")
        folderId = intent.getStringExtra("FOLDER_ID")
        // If folderId is empty string, treat as null (safe Kotlin idiom)
        folderId = folderId?.takeIf { it.isNotBlank() }

        // Checklist logic
        fun showChecklistMode(show: Boolean) {
            isChecklistMode = show
            checklistContainer.visibility = if (show) View.VISIBLE else View.GONE
            tilContent.visibility = if (show) View.GONE else View.VISIBLE
        }

        switchChecklist.setOnCheckedChangeListener { _, isChecked ->
            showChecklistMode(isChecked)
        }

        btnAddChecklistItem.setOnClickListener {
            val itemEditText = EditText(this)
            itemEditText.hint = "Checklist item"
            itemEditText.setBackgroundResource(R.drawable.rounded_card)
            itemEditText.setPadding(32, 24, 32, 24)
            itemEditText.textSize = 16f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 16, 0, 16)
            itemEditText.layoutParams = params
            checklistContainer.addView(itemEditText, checklistContainer.childCount - 1)
            checklistEditTexts.add(itemEditText)
        }

        // If editing, load entry
        if (entryId != null) {
            deleteButton.visibility = View.VISIBLE
            db.collection("users").document(userId).collection("entries").document(entryId!!)
                .get()
                .addOnSuccessListener { document ->
                    val entry = document.toObject(DiaryEntry::class.java)
                    if (entry != null) {
                        titleEditText.setText(entry.title)
                        if (!entry.checklist.isNullOrEmpty()) {
                            switchChecklist.isChecked = true
                            showChecklistMode(true)
                            // Populate checklist
                            entry.checklist.forEach { item ->
                                val itemEditText = EditText(this)
                                itemEditText.setText(item)
                                itemEditText.hint = "Checklist item"
                                itemEditText.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 8, 0, 8) }
                                checklistContainer.addView(itemEditText, checklistContainer.childCount - 1)
                                checklistEditTexts.add(itemEditText)
                            }
                        } else {
                            switchChecklist.isChecked = false
                            showChecklistMode(false)
                            contentEditText.setText(entry.content ?: "")
                        }
                    }
                }
        } else {
            deleteButton.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val id = entryId ?: db.collection("users").document(userId).collection("entries").document().id
            val timestamp = System.currentTimeMillis()
            // Always use the folderId from the intent (null if not in a folder)
            if (isChecklistMode) {
                val checklist = checklistEditTexts.map { it.text.toString().trim() }.filter { it.isNotEmpty() }
                if (checklist.isEmpty()) {
                    Toast.makeText(this, "Add at least one checklist item", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val entry = DiaryEntry(
                    id = id,
                    title = title,
                    checklist = checklist,
                    content = null,
                    folderId = folderId,
                    timestamp = timestamp
                )
                db.collection("users").document(userId).collection("entries").document(id)
                    .set(entry)
                    .addOnSuccessListener { finish() }
                    .addOnFailureListener { Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show() }
            } else {
                val content = contentEditText.text.toString().trim()
                if (content.isEmpty()) {
                    Toast.makeText(this, "Content required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val entry = DiaryEntry(
                    id = id,
                    title = title,
                    content = content,
                    checklist = null,
                    folderId = folderId,
                    timestamp = timestamp
                )
                db.collection("users").document(userId).collection("entries").document(id)
                    .set(entry)
                    .addOnSuccessListener { finish() }
                    .addOnFailureListener { Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show() }
            }
        }

        deleteButton.setOnClickListener {
            if (entryId != null) {
                db.collection("users").document(userId).collection("entries").document(entryId!!)
                    .delete()
                    .addOnSuccessListener { finish() }
                    .addOnFailureListener { Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}