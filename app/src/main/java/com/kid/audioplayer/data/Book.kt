package com.kid.audioplayer.data

import java.io.File

data class Book(
    val id: String,
    val source: String,
    val directory: File,
    val title: String,
    val cover: String,
    val units: List<BookUnit>,
)

data class BookUnit(val title: String, val audio: String, val pages: List<BookPage>)

data class BookPage(val image: String, val startMs: Long, val endMs: Long)
