package com.vishnu.threatsurfaceshield

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val historyRecyclerView = findViewById<RecyclerView>(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        // Get history from the persistent HistoryManager
        val history = HistoryManager.getHistory()
        historyRecyclerView.adapter = HistoryAdapter(history)
    }
}
