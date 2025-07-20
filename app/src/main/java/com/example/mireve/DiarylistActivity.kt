package com.example.mireve

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.TextView

class DiaryListActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: DiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        // Removed setSupportActionBar(findViewById(R.id.toolbar)) as there is no Toolbar in the layout.
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = DiaryAdapter(
            onItemClick = { entry ->
                val intent = Intent(this, AddEditDiaryActivity::class.java)
                intent.putExtra("ENTRY_ID", entry.id)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        db.collection("users").document(userId).collection("entries")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                val entries = value?.toObjects(DiaryEntry::class.java) ?: emptyList()
                adapter.submitList(entries)
                findViewById<TextView>(R.id.tvNoteCount).text = "${entries.size} note" + if (entries.size == 1) "" else "s"
            }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditDiaryActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_diary_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, UserProfileActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}