package com.example.mireve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DiaryAdapter(
    private val onItemClick: (DiaryEntry) -> Unit,
    private val onDeleteClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DiffCallback()) {

    private var selectedPosition: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry, position == selectedPosition)
        holder.itemView.setOnClickListener { onItemClick(entry) }
        holder.itemView.setOnLongClickListener {
            selectedPosition = if (selectedPosition == position) null else position
            notifyDataSetChanged()
            true
        }
        holder.deleteButton.setOnClickListener {
            onDeleteClick(entry)
            selectedPosition = null
            notifyDataSetChanged()
        }
    }

    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
        fun bind(entry: DiaryEntry, showDelete: Boolean) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = entry.title
            itemView.findViewById<TextView>(R.id.tvContent).text = entry.content
            val dateText = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", entry.timestamp)
            itemView.findViewById<TextView>(R.id.tvDate).text = dateText
            deleteButton.visibility = if (showDelete) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DiaryEntry>() {
        override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) = oldItem == newItem
    }
}