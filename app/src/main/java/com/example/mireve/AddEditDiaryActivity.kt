package com.example.mireve

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.OnBackPressedCallback


class AddEditDiaryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var toolbar: Toolbar
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    // Removed switchType
    // Removed checklistAdapter
    // Removed recyclerChecklist
    // Removed cardChecklistContent
    private lateinit var cardTextContent: View // This will always be visible now
    // Removed btnAddChecklistItem
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var entryId: String? = null
    private var isEditing = false
    private var hasUnsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_diary)

        initializeViews()
        setupToolbar()
        setupListeners()
        setupBackPressHandler()
        loadEntry()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        titleEditText = findViewById(R.id.etTitle)
        contentEditText = findViewById(R.id.etContent)
        // Removed references to checklist-related views
        // switchType = findViewById(R.id.switchType)
        // cardChecklistContent = findViewById(R.id.cardChecklistContent)
        cardTextContent = findViewById(R.id.cardTextContent)
        // recyclerChecklist = findViewById(R.id.recyclerChecklist)
        // btnAddChecklistItem = findViewById(R.id.btnAddChecklistItem)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Removed checklistAdapter initialization
        // checklistAdapter = ChecklistAdapter(...)
        // Removed recyclerChecklist setup
        // recyclerChecklist.layoutManager = LinearLayoutManager(this)
        // recyclerChecklist.adapter = checklistAdapter

        // Apply window insets to toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Set initial state (only text content is visible now)
        // switchType.isChecked = false // No switch
        cardTextContent.visibility = View.VISIBLE
        // cardChecklistContent.visibility = View.GONE // No checklist content
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Note"
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    showUnsavedChangesDialog()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupListeners() {
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
                updateToolbarTitle()
            }
        })

        contentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
            }
        })

        // Removed switchType.setOnCheckedChangeListener
        // Removed btnAddChecklistItem.setOnClickListener

        btnSave.setOnClickListener {
            saveEntry()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadEntry() {
        entryId = intent.getStringExtra("ENTRY_ID")
        if (entryId != null) {
            isEditing = true
            supportActionBar?.title = "Edit Note"
            btnDelete.visibility = View.VISIBLE

            val userId = auth.currentUser?.uid ?: return
            db.collection("users").document(userId).collection("entries")
                .document(entryId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val entry = document.toObject(DiaryEntry::class.java)
                        entry?.let {
                            titleEditText.setText(it.title)
                            contentEditText.setText(it.content)
                            // Removed checklist-specific loading logic
                            // switchType.isChecked = it.isChecklist
                            // if (it.isChecklist) {
                            //     cardTextContent.visibility = View.GONE
                            //     cardChecklistContent.visibility = View.VISIBLE
                            //     checklistAdapter.submitList(it.checklistItems?.toMutableList() ?: emptyList())
                            // }
                            updateToolbarTitle()
                            hasUnsavedChanges = false // Reset after loading
                        }
                    }
                }
        }
    }

    private fun updateToolbarTitle() {
        val title = titleEditText.text.toString().trim()
        val mode = if (isEditing) "Edit" else "New"
        // Simplified type as there's only "Note" now
        val type = "Note"

        val toolbarTitle = if (title.isNotEmpty()) {
            val truncatedTitle = if (title.length > 25) title.substring(0, 25) + "..." else title
            "$mode $type: $truncatedTitle"
        } else {
            "$mode $type"
        }

        supportActionBar?.title = toolbarTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them?")
            .setPositiveButton("Save") { _, _ ->
                saveEntry()
            }
            .setNegativeButton("Discard") { _, _ ->
                hasUnsavedChanges = false // Discard changes, so no save prompt again
                finish()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveEntry() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        // Removed isChecklistMode variable

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Only save plain text content
        if (content.isEmpty()) {
            Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        saveToFirestore(
            DiaryEntry(
                id = entryId ?: "",
                title = title,
                content = content,
                // Removed isChecklist = false,
                // Removed checklistItems = null,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun saveToFirestore(entry: DiaryEntry) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val collectionRef = db.collection("users").document(userId).collection("entries")

        if (isEditing) {
            collectionRef.document(entry.id).set(entry)
                .addOnSuccessListener {
                    Toast.makeText(this, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                    hasUnsavedChanges = false
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update entry: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // When adding, let Firestore generate the ID, then update the object with it
            collectionRef.add(entry)
                .addOnSuccessListener { docRef ->
                    val updatedEntry = entry.copy(id = docRef.id)
                    collectionRef.document(docRef.id).set(updatedEntry) // Set with the new ID
                        .addOnSuccessListener {
                            Toast.makeText(this, "Entry saved successfully", Toast.LENGTH_SHORT).show()
                            hasUnsavedChanges = false
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update entry with ID: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save new entry: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEntry()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(userId).collection("entries")
            .document(entryId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Entry deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show()
            }
    }
}
