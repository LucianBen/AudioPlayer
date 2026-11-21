package com.kid.audioplayer.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kid.audioplayer.data.Book
import com.kid.audioplayer.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CatalogViewModel(repository: BookRepository, bookId: String) : ViewModel() {
    val book = repository.books.map { books -> books.firstOrNull { it.id == bookId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null as Book?)
}
