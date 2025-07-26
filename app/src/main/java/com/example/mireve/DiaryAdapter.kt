package com.example.mireve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class DiaryAdapter(
    private val onItemClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DiaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val contentContainer: LinearLayout = itemView.findViewById(R.id.contentContainer)

        fun bind(entry: DiaryEntry) {
            tvTitle.text = entry.title
            tvDate.text = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                .format(Date(entry.timestamp))

            contentContainer.removeAllViews()

            if (entry.isChecklist) {
                val checklistItems = entry.checklistItems ?: emptyList()
                for (item in checklistItems) {
                    val checkbox = CheckBox(itemView.context)
                    checkbox.text = item.text
                    checkbox.isChecked = item.isCompleted
                    checkbox.isEnabled = false
                    contentContainer.addView(checkbox)
                }
            } else {
                val textView = TextView(itemView.context)
                textView.text = entry.content
                textView.textSize = 16f
                textView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                textView.setPadding(0, 8, 0, 8)
                contentContainer.addView(textView)
            }

            cardView.setOnClickListener {
                onItemClick(entry)
            }
        }
    }
}

class DiaryDiffCallback : DiffUtil.ItemCallback<DiaryEntry>() {
    override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean {
        return oldItem == newItem
    }
}