package com.kid.audioplayer.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.audioplayer.data.Book
import com.kid.audioplayer.data.BookRepository
import com.kid.audioplayer.player.PlaybackClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONException

data class LibraryState(val books: List<Book> = emptyList(), val busy: Boolean = true, val message: String? = null)

class LibraryViewModel(
    private val repository: BookRepository,
    private val playback: PlaybackClient,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    val state = combine(repository.books, busy, message, ::LibraryState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryState())

    init { runOperation("无法读取书库") { repository.load() } }

    fun importBook(uri: Uri) = runOperation("导入失败") { repository.importBook(uri, playback::release) }

    fun deleteBook(id: String) = runOperation("删除失败") { repository.delete(id, playback::release) }

    fun dismissMessage() { message.value = null }

    private fun runOperation(failure: String, operation: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: JSONException) {
                message.value = "$failure：目录 JSON 无效"
            } catch (error: Exception) {
                message.value = error.message?.let { "$failure：$it" } ?: failure
            } finally {
                busy.value = false
            }
        }
    }
}
