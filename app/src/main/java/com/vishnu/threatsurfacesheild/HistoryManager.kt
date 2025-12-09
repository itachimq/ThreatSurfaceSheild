package com.vishnu.threatsurfaceshield

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class HistoryItem(
    val url: String,
    val verdict: String,
    val score: Int,
    val timestamp: Long
)

object HistoryManager {

    private const val HISTORY_FILE = "history.json"
    private var history = mutableListOf<HistoryItem>()

    init {
        loadHistory()
    }

    fun addHistory(item: HistoryItem) {
        history.add(0, item)
        if (history.size > 100) {
            history.removeAt(history.lastIndex)
        }
        saveHistory()
    }

    fun getHistory(): List<HistoryItem> {
        return history
    }

    private fun saveHistory() {
        val file = File(App.appContext.filesDir, HISTORY_FILE)
        try {
            val gson = Gson()
            val json = gson.toJson(history)
            FileWriter(file).use { it.write(json) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadHistory() {
        val file = File(App.appContext.filesDir, HISTORY_FILE)
        if (file.exists()) {
            try {
                FileReader(file).use { reader ->
                    val gson = Gson()
                    val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
                    history = gson.fromJson(reader, type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
