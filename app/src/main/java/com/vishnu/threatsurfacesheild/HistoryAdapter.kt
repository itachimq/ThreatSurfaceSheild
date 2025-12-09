package com.vishnu.threatsurfaceshield

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val history: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val urlTextView: TextView = view.findViewById(R.id.urlTextView)
        val verdictTextView: TextView = view.findViewById(R.id.verdictTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = history[position]
        holder.urlTextView.text = item.url
        holder.verdictTextView.text = "Risk: ${item.verdict} (${item.score})"

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        holder.timestampTextView.text = sdf.format(Date(item.timestamp))
    }

    override fun getItemCount() = history.size
}
