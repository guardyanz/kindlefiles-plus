// SPDX-License-Identifier: Apache-2.0
package io.github.guardyanz.kindlefiles.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.guardyanz.kindlefiles.R
import io.github.guardyanz.kindlefiles.data.SafStore
import io.github.guardyanz.kindlefiles.ui.KindleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FolderWidgetConfiguration : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) { finish(); return@registerForActivityResult }
        val data = result.data ?: return@registerForActivityResult
        val uri = data.data ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    val grants = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(uri, grants)
                    SafStore(applicationContext).stat(SafStore.treeDocument(uri))
                }
                getSharedPreferences("widgets", MODE_PRIVATE).edit().putString("uri_$widgetId", document.id)
                    .putString("name_$widgetId", document.name).apply()
                FolderWidget.update(this@FolderWidgetConfiguration, AppWidgetManager.getInstance(this@FolderWidgetConfiguration), widgetId)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            } catch (_: Exception) {
                android.widget.Toast.makeText(this@FolderWidgetConfiguration, R.string.access_error, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setContent { KindleTheme {
            Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.widget_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.connect_hint))
                Button(onClick = {
                    try { picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)) }
                    catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(this@FolderWidgetConfiguration, R.string.no_picker, android.widget.Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.widget_configure)) }
            } }
        } }
    }
}
