package com.example.mireve

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DiaryAdapter(
    private val onItemClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentContainer: LinearLayout = itemView.findViewById(R.id.contentContainer)
        fun bind(entry: DiaryEntry) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = entry.title
            val dateText = android.text.format.DateFormat.format("MMM d", entry.timestamp)
            itemView.findViewById<TextView>(R.id.tvDate).text = dateText
            // Remove all previous views
            contentContainer.removeAllViews()
            val context = itemView.context
            if (!entry.checklist.isNullOrEmpty()) {
                // Show checklist items
                entry.checklist.forEach { item ->
                    val row = LinearLayout(context)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.setPadding(0, 4, 0, 4)
                    // Add square checkbox
                    val checkBoxView = View(context)
                    val size = (20 * context.resources.displayMetrics.density).toInt()
                    val params = LinearLayout.LayoutParams(size, size)
                    params.setMargins(0, 0, 16, 0)
                    checkBoxView.layoutParams = params
                    if (item.startsWith("[x] ")) {
                        checkBoxView.setBackgroundResource(android.R.drawable.checkbox_on_background)
                    } else {
                        checkBoxView.setBackgroundResource(android.R.drawable.checkbox_off_background)
                    }
                    row.addView(checkBoxView)
                    // Checklist text
                    val text = TextView(context)
                    text.text = if (item.startsWith("[x] ")) item.removePrefix("[x] ") else item
                    text.textSize = 16f
                    text.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    if (item.startsWith("[x] ")) {
                        text.paintFlags = text.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        text.paintFlags = text.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    row.addView(text)
                    contentContainer.addView(row)
                }
            } else if (!entry.content.isNullOrBlank()) {
                // Show content as italic/quote
                val text = TextView(context)
                val spannable = SpannableString(entry.content)
                spannable.setSpan(StyleSpan(Typeface.ITALIC), 0, spannable.length, 0)
                text.text = spannable
                text.textSize = 16f
                text.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                contentContainer.addView(text)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DiaryEntry>() {
        override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) = oldItem == newItem
    }
}