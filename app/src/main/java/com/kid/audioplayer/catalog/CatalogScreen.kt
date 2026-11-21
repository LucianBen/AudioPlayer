package com.kid.audioplayer.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val unitPrefix = Regex("""^Unit\s+\d+\b""", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(model: CatalogViewModel, onBack: () -> Unit, openUnit: (Int) -> Unit) {
    val book by model.book.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text(book?.title ?: "目录") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回书库") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            itemsIndexed(book?.units.orEmpty()) { index, unit ->
                val prefix = unitPrefix.find(unit.title)?.value
                val heading = prefix?.let { unit.title.removePrefix(it).trimStart() }.orEmpty()
                ListItem(
                    headlineContent = {
                        if (prefix == null) Text(unit.title)
                        else if (heading.isNotEmpty()) Text(heading)
                        else UnitLabel(prefix)
                    },
                    leadingContent = if (prefix != null && heading.isNotEmpty()) {
                        { UnitLabel(prefix) }
                    } else null,
                    modifier = Modifier.fillMaxWidth().clickable { openUnit(index) },
                )
            }
        }
    }
}

@Composable
private fun UnitLabel(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}
