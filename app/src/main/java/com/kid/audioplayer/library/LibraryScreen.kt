package com.kid.audioplayer.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kid.audioplayer.BookImage
import com.kid.audioplayer.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: LibraryViewModel, openBook: (String) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        it?.let(model::importBook)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("书库") }, actions = {
            IconButton(onClick = { picker.launch(null) }, enabled = !state.busy) {
                Icon(painterResource(R.drawable.ic_folder_open), "导入书籍目录")
            }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.books.isEmpty() && !state.busy) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("书库为空。点击右上角的文件夹导入书籍。")
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(144.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.books, key = { it.id }) { book ->
                    Card {
                        Column(Modifier.fillMaxWidth().combinedClickable(
                            enabled = !state.busy,
                            onClick = { openBook(book.id) },
                            onLongClickLabel = "删除书籍",
                            onLongClick = { deletingId = book.id },
                        ).padding(10.dp)) {
                            BookImage(File(book.directory, book.cover), "封面", Modifier.fillMaxWidth().aspectRatio(0.85f))
                            Text(book.title, Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }

    state.books.firstOrNull { it.id == deletingId }?.let { book ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("删除《${book.title}》？") },
            text = { Text("书籍将从书库和应用存储中移除。") },
            confirmButton = { TextButton(onClick = { deletingId = null; model.deleteBook(book.id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("取消") } },
        )
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = model::dismissMessage,
            text = { Text(message) },
            confirmButton = { TextButton(onClick = model::dismissMessage) { Text("确定") } },
        )
    }
}
