package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.EditText
import android.widget.CheckBox
import com.google.android.material.switchmaterial.SwitchMaterial

class AddEditDiaryActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var entryId: String? = null
    private var folderId: String? = null
    private lateinit var checklistAdapter: ChecklistAdapter

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
        val switchChecklist = findViewById<SwitchMaterial>(R.id.switchChecklist)
        val btnAddChecklistItem = findViewById<Button>(R.id.btnAddChecklistItem)
        btnAddChecklistItem.visibility = View.GONE
        val recyclerChecklist = findViewById<RecyclerView>(R.id.recyclerChecklist)
        val etContent = findViewById<EditText>(R.id.etContent)
        val cardTextContent = findViewById<View>(R.id.cardTextContent)
        val cardChecklistContent = findViewById<View>(R.id.cardChecklistContent)

        val checklistItems = mutableListOf<String>()
        checklistAdapter = ChecklistAdapter(checklistItems) { pos ->
            checklistItems.removeAt(pos)
            checklistAdapter.notifyItemRemoved(pos)
        }
        recyclerChecklist.layoutManager = LinearLayoutManager(this)
        recyclerChecklist.adapter = checklistAdapter

        switchChecklist.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cardChecklistContent.visibility = View.VISIBLE
                cardTextContent.visibility = View.GONE
                btnAddChecklistItem.visibility = View.VISIBLE
            } else {
                cardChecklistContent.visibility = View.GONE
                cardTextContent.visibility = View.VISIBLE
                btnAddChecklistItem.visibility = View.GONE
            }
        }
        btnAddChecklistItem.setOnClickListener {
            checklistItems.add("")
            checklistAdapter.notifyItemInserted(checklistItems.size - 1)
        }

        entryId = intent.getStringExtra("ENTRY_ID")
        folderId = intent.getStringExtra("FOLDER_ID")
        folderId = folderId?.takeIf { it.isNotBlank() }

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
                            checklistItems.addAll(entry.checklist)
                            checklistAdapter.notifyDataSetChanged()
                            switchChecklist.isChecked = true
                        } else {
                            switchChecklist.isChecked = false
                        }
                        contentEditText.setText(entry.content ?: "")
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
            if (switchChecklist.isChecked) {
                val checklist = checklistItems.filter { it.isNotBlank() }
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

class ChecklistAdapter(
    private val items: MutableList<String>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {
    inner class ChecklistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val editText: EditText = view.findViewById(R.id.editText)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }
    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.editText.setText(items[position])
        holder.editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items[pos] = s.toString()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(pos)
            }
        }
    }
    override fun getItemCount() = items.size
}