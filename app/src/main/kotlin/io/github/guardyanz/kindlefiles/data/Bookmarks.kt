// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.data

import android.content.Context
import io.github.guardyanz.kindlefiles.core.Document
import org.json.JSONArray
import org.json.JSONObject

class Bookmarks(context: Context) {
    private val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)
    fun read(key: String): List<Document> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { index -> array.getJSONObject(index).let {
            Document(it.getString("id"), it.getString("name"), true)
        } }
    }.getOrDefault(emptyList())
    fun write(key: String, documents: List<Document>) {
        val array = JSONArray()
        documents.distinctBy { it.id }.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name)) }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
