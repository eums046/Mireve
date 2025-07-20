package com.example.mireve

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.LayoutInflater
import android.view.ViewGroup

class DiaryListActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: DiaryAdapter
    private var showingFolders = false
    private var folders: List<Folder> = emptyList()
    private var viewingFolder: Folder? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabAllEntries: TextView
    private lateinit var tabFolders: TextView
    private lateinit var tvNoteCount: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var folderAdapter: RecyclerView.Adapter<FolderViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check login only once when activity is created
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }
        setContentView(R.layout.activity_diary_list)

        setSupportActionBar(findViewById(R.id.toolbar))
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        recyclerView = findViewById(R.id.recyclerView)
        tabAllEntries = findViewById(R.id.tabAllEntries)
        tabFolders = findViewById(R.id.tabFolders)
        tvNoteCount = findViewById(R.id.tvNoteCount)
        fabAdd = findViewById(R.id.fabAdd)
        toolbar = findViewById(R.id.toolbar)

        adapter = DiaryAdapter(
            onItemClick = { entry ->
                val intent = Intent(this, AddEditDiaryActivity::class.java)
                intent.putExtra("ENTRY_ID", entry.id)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        tabAllEntries.setOnClickListener { showNotes() }
        tabFolders.setOnClickListener { showFolders() }
        showNotes()

        fabAdd.setOnClickListener {
            val options = arrayOf("Add Note", "Add Folder")
            AlertDialog.Builder(this)
                .setTitle("Add...")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        val intent = Intent(this, AddEditDiaryActivity::class.java)
                        viewingFolder?.let { intent.putExtra("FOLDER_ID", it.id) }
                        startActivity(intent)
                    } else {
                        val input = EditText(this)
                        input.hint = "Folder name"
                        AlertDialog.Builder(this)
                            .setTitle("New Folder")
                            .setView(input)
                            .setPositiveButton("Create") { _, _ ->
                                val name = input.text.toString().trim()
                                if (name.isNotEmpty()) {
                                    val id = db.collection("users").document(userId).collection("folders").document().id
                                    val folder = Folder(id = id, name = name, timestamp = System.currentTimeMillis())
                                    db.collection("users").document(userId).collection("folders").document(id).set(folder)
                                } else {
                                    Toast.makeText(this, "Folder name required", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                .show()
        }
    }

    private fun showNotes(folder: Folder? = null) {
        val userId = auth.currentUser?.uid ?: return
        showingFolders = false
        viewingFolder = folder
        folderAdapter = null // Clear folder adapter reference
        recyclerView.adapter = adapter
        tabAllEntries.setBackgroundResource(R.drawable.rounded_tab_selected)
        tabFolders.setBackgroundResource(0)
        supportActionBar?.setDisplayHomeAsUpEnabled(folder != null)
        supportActionBar?.title = folder?.name ?: ""
        val query = db.collection("users").document(userId).collection("entries")
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val filteredQuery = if (folder != null) query.whereEqualTo("folderId", folder.id) else query
        filteredQuery.addSnapshotListener { value, _ ->
            val entries = value?.toObjects(DiaryEntry::class.java) ?: emptyList()
            adapter.submitList(entries)
            tvNoteCount.text = "${entries.size} note" + if (entries.size == 1) "" else "s"
        }
    }

    private fun showFolders() {
        val userId = auth.currentUser?.uid ?: return
        showingFolders = true
        viewingFolder = null
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = ""
        db.collection("users").document(userId).collection("folders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                folders = value?.toObjects(Folder::class.java) ?: emptyList()
                tvNoteCount.text = "${folders.size} folder" + if (folders.size == 1) "" else "s"
                folderAdapter = object : RecyclerView.Adapter<FolderViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
                        val card = com.google.android.material.card.MaterialCardView(parent.context)
                        card.layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 16
                            bottomMargin = 16
                            leftMargin = 16
                            rightMargin = 16
                        }
                        card.radius = 28f
                        card.cardElevation = 8f
                        card.setCardBackgroundColor(parent.context.getColor(android.R.color.white))
                        val text = TextView(parent.context)
                        text.textSize = 20f
                        text.setPadding(32, 32, 32, 32)
                        text.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        card.addView(text)
                        return FolderViewHolder(card)
                    }
                    override fun getItemCount() = folders.size
                    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
                        holder.textView.text = folders[position].name
                        holder.itemView.setOnClickListener {
                            showNotes(folders[position])
                        }
                    }
                }
                recyclerView.adapter = folderAdapter
            }
        tabFolders.setBackgroundResource(R.drawable.rounded_tab_selected)
        tabAllEntries.setBackgroundResource(0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_diary_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && viewingFolder != null) {
            // Back arrow pressed, return to folder list
            showFolders()
            return true
        }
        if (item.itemId == R.id.action_profile) {
            startActivity(Intent(this, UserProfileActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textView: TextView = (view as ViewGroup).getChildAt(0) as TextView
}