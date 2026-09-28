// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.guardyanz.kindlefiles.MainActivity
import io.github.guardyanz.kindlefiles.R

class FolderWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    override fun onDeleted(context: Context, ids: IntArray) {
        val editor = context.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit()
        ids.forEach { editor.remove("uri_$it").remove("name_$it") }
        editor.apply()
    }
    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
            val uri = prefs.getString("uri_$id", null)
            val launch = Intent(context, MainActivity::class.java).apply {
                putExtra("folder", uri)
                // Each widget has its own immutable PendingIntent identity.
                data = android.net.Uri.parse("kindlefiles://widget/$id")
            }
            val pending = PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val views = RemoteViews(context.packageName, R.layout.folder_widget).apply {
                setTextViewText(R.id.widget_name, prefs.getString("name_$id", context.getString(R.string.widget_open)))
                setOnClickPendingIntent(R.id.widget_root, pending)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
