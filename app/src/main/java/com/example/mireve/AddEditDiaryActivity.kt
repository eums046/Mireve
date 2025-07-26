package com.example.mireve

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class AddEditDiaryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var toolbar: Toolbar
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var switchType: SwitchMaterial
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var recyclerChecklist: RecyclerView
    private lateinit var cardChecklistContent: View
    private lateinit var cardTextContent: View
    private lateinit var btnAddChecklistItem: Button
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
        loadEntry()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        titleEditText = findViewById(R.id.etTitle)
        contentEditText = findViewById(R.id.etContent)
        switchType = findViewById<SwitchMaterial>(R.id.switchType)
        cardChecklistContent = findViewById(R.id.cardChecklistContent)
        cardTextContent = findViewById(R.id.cardTextContent)
        recyclerChecklist = findViewById(R.id.recyclerChecklist)
        btnAddChecklistItem = findViewById(R.id.btnAddChecklistItem)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        checklistAdapter = ChecklistAdapter(
            onItemChanged = { hasUnsavedChanges = true },
            onItemDeleted = { position ->
                hasUnsavedChanges = true
                checklistAdapter.removeItem(position)
            }
        )

        recyclerChecklist.layoutManager = LinearLayoutManager(this)
        recyclerChecklist.adapter = checklistAdapter

        // Apply window insets to toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Note"
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

        switchType.setOnCheckedChangeListener { _, isChecked ->
            hasUnsavedChanges = true
            if (isChecked) {
                cardTextContent.visibility = View.GONE
                cardChecklistContent.visibility = View.VISIBLE
            } else {
                cardTextContent.visibility = View.VISIBLE
                cardChecklistContent.visibility = View.GONE
            }
        }

        btnAddChecklistItem.setOnClickListener {
            hasUnsavedChanges = true
            checklistAdapter.addItem(ChecklistItem("", false))
        }

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
                            switchType.isChecked = it.isChecklist
                            if (it.isChecklist) {
                                cardTextContent.visibility = View.GONE
                                cardChecklistContent.visibility = View.VISIBLE
                                checklistAdapter.submitList(it.checklistItems ?: emptyList())
                            }
                            updateToolbarTitle()
                        }
                    }
                }
        }
    }

    private fun updateToolbarTitle() {
        val title = titleEditText.text.toString().trim()
        val mode = if (isEditing) "Edit" else "New"
        val type = if (switchType.isChecked) "Checklist" else "Note"

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

    override fun onBackPressed() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            super.onBackPressed()
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
                super.onBackPressed()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveEntry() {
        val title = titleEditText.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val entry = DiaryEntry(
            id = entryId ?: "",
            title = title,
            content = if (switchType.isChecked) null else contentEditText.text.toString().trim(),
            isChecklist = switchType.isChecked,
            checklistItems = if (switchType.isChecked) checklistAdapter.currentList else null,
            timestamp = System.currentTimeMillis()
        )

        val collectionRef = db.collection("users").document(userId).collection("entries")

        if (isEditing) {
            collectionRef.document(entryId!!).set(entry)
                .addOnSuccessListener {
                    Toast.makeText(this, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update entry", Toast.LENGTH_SHORT).show()
                }
        } else {
            collectionRef.add(entry)
                .addOnSuccessListener { documentReference ->
                    val updatedEntry = entry.copy(id = documentReference.id)
                    collectionRef.document(documentReference.id).set(updatedEntry)
                    Toast.makeText(this, "Entry saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show()
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
        val userId = auth.currentUser?.uid ?: return
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

class ChecklistAdapter(
    private val onItemChanged: () -> Unit,
    private val onItemDeleted: (Int) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    private var items = mutableListOf<ChecklistItem>()

    fun submitList(newItems: List<ChecklistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: ChecklistItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (position in 0 until items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    val currentList: List<ChecklistItem>
        get() = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        private val editText: EditText = itemView.findViewById(R.id.editText)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(item: ChecklistItem, position: Int) {
            checkBox.isChecked = item.isCompleted
            editText.setText(item.text)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                items[position] = item.copy(isCompleted = isChecked)
                onItemChanged()
            }

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    items[position] = item.copy(text = s.toString())
                    onItemChanged()
                }
            })

            btnDelete.setOnClickListener {
                onItemDeleted(position)
            }
        }
    }
}