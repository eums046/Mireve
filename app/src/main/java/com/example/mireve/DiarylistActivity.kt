package com.example.mireve

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.LayoutInflater
import android.view.ViewGroup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.os.Handler
import android.os.Looper

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
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var ivProfile: ImageView
    private lateinit var ivAbout: ImageView
    private var folderAdapter: RecyclerView.Adapter<FolderViewHolder>? = null
    private lateinit var tvQuoteOfTheDay: TextView
    private lateinit var tvQuoteAuthor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val user = FirebaseAuth.getInstance().currentUser
        val isLoggedIn = LoginManager.isLoggedIn(this)
        val isSessionValid = LoginManager.isSessionValid(this)
        
        if (user == null && !isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        } else if (user == null && isLoggedIn && isSessionValid) {
            Log.d("DiaryListActivity", "Firebase session null but local login valid, attempting restore...")
            attemptSessionRestore()
            return
        } else if (user == null) {
            LoginManager.clearLoginState(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }
        setContentView(R.layout.activity_diary_list)
        initializeActivity()
    }

    override fun onResume() {
        super.onResume()
        fetchQuoteOfTheDay()
    }

    private fun fetchQuoteOfTheDay() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://zenquotes.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(QuoteApi::class.java)
        service.getQuote().enqueue(object : Callback<List<QuoteResponse>> {
            override fun onResponse(call: Call<List<QuoteResponse>>, response: Response<List<QuoteResponse>>) {
                val quote = response.body()?.firstOrNull()
                if (quote != null) {
                    tvQuoteOfTheDay.text = "\"${quote.q}\""
                    tvQuoteAuthor.text = "- ${quote.a}"
                    tvQuoteAuthor.visibility = View.VISIBLE
                } else {
                    tvQuoteOfTheDay.text = getString(R.string.quote_load_error)
                    tvQuoteAuthor.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<List<QuoteResponse>>, t: Throwable) {
                tvQuoteOfTheDay.text = getString(R.string.quote_load_error)
                tvQuoteAuthor.visibility = View.GONE
            }
        })
    }

    private fun showNotes(folder: Folder? = null) {
        val userId = auth.currentUser?.uid ?: return
        showingFolders = false
        viewingFolder = folder
        folderAdapter = null
        recyclerView.adapter = adapter

        tabAllEntries.setBackgroundResource(R.drawable.rounded_tab_selected)
        tabAllEntries.setTextColor(getColor(android.R.color.white))
        tabFolders.background = null
        tabFolders.setTextColor(getColor(R.color.black))

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
                        text.setTextColor(parent.context.getColor(android.R.color.black))
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
        tabFolders.setTextColor(getColor(android.R.color.white))
        tabAllEntries.background = null
        tabAllEntries.setTextColor(getColor(R.color.black))
    }
    
    private fun attemptSessionRestore() {
        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d("DiaryListActivity", "Firebase session restored for user: ${user.email}")
                LoginManager.refreshLoginState(this, user.email ?: "")
                setContentView(R.layout.activity_diary_list)
                initializeActivity()
            } else {
                Log.d("DiaryListActivity", "Firebase session couldn't be restored, redirecting to login")
                LoginManager.clearLoginState(this)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }, 1000)
    }
    
    private fun initializeActivity() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        recyclerView = findViewById(R.id.recyclerView)
        tabAllEntries = findViewById(R.id.tabAllEntries)
        tabFolders = findViewById(R.id.tabFolders)
        tvNoteCount = findViewById(R.id.tvNoteCount)
        fabAdd = findViewById(R.id.fabAdd)
        ivProfile = findViewById(R.id.ivProfile)
        ivAbout = findViewById(R.id.ivAbout)
        tvQuoteOfTheDay = findViewById(R.id.tvQuoteOfTheDay)
        tvQuoteAuthor = findViewById(R.id.tvQuoteAuthor)

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

        ivProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        ivAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        showNotes()

        fabAdd.setOnClickListener {
            val options = arrayOf("Add Note", "Add Folder")
            AlertDialog.Builder(this)
                .setTitle("Add...")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(this, AddEditDiaryActivity::class.java)
                            startActivity(intent)
                        }
                        1 -> {
                            val folderName = EditText(this).apply {
                                hint = "Folder name"
                            }
                            AlertDialog.Builder(this)
                                .setTitle("Create Folder")
                                .setView(folderName)
                                .setPositiveButton("Create") { _, _ ->
                                    val name = folderName.text.toString()
                                    if (name.isNotEmpty()) {
                                        val folder = Folder(
                                            id = "",
                                            name = name,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        db.collection("users").document(userId).collection("folders")
                                            .add(folder)
                                            .addOnSuccessListener { documentReference ->
                                                val updatedFolder = folder.copy(id = documentReference.id)
                                                db.collection("users").document(userId).collection("folders")
                                                    .document(documentReference.id)
                                                    .set(updatedFolder)
                                            }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                .show()
        }

        loadQuoteOfTheDay()
    }

    private fun loadQuoteOfTheDay() {
        fetchQuoteOfTheDay()
    }
}

class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textView: TextView = (view as ViewGroup).getChildAt(0) as TextView
}

interface QuoteApi {
    @GET("api/today")
    fun getQuote(): Call<List<QuoteResponse>>
}
data class QuoteResponse(
    val q: String,
    val a: String
)