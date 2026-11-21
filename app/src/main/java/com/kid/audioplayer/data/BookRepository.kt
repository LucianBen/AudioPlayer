package com.kid.audioplayer.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BookRepository(context: Context) {
    private val resolver = context.contentResolver
    private val root = File(context.filesDir, "books")
    private val index = AtomicFile(File(root, "library.json"))
    private val mutations = Mutex()
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()
    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {
        mutations.withLock { loadLocked() }
    }

    suspend fun importBook(tree: Uri, release: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        load()
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val source = DocumentsContract.buildDocumentUriUsingTree(tree, treeId).toString()
        val entries = children(tree, treeId)
        val catalogs = entries.values.filter { !it.directory && it.name.endsWith(".json", true) }
        require(catalogs.size == 1) { "所选目录必须包含一个 JSON 文件" }
        val catalog = catalogs.single()
        val json = read(tree, catalog) { it.bufferedReader(Charsets.UTF_8).readText() }
        val (title, cover, units) = BookParser.parse(json)
        checkUnique(title)
        val directories = mutableMapOf("" to entries)
        fun resolve(path: String): Entry {
            var parent = ""
            var listing = entries
            val parts = path.split('/')
            for (name in parts.dropLast(1)) {
                val entry = listing[name]?.takeIf { it.directory }
                    ?: throw IllegalArgumentException("找不到目录：$path")
                parent += "/$name"
                listing = directories.getOrPut(parent) { children(tree, entry.id) }
            }
            return listing[parts.last()]?.takeUnless { it.directory }
                ?: throw IllegalArgumentException("找不到文件：$path")
        }

        val images = (listOf(cover) + units.flatMap { unit -> unit.pages.map { it.image } }).toSet()
        val audio = units.map { it.audio }.toSet()
        val resources = (images + audio).associateWith(::resolve)
        images.forEach { path ->
            currentCoroutineContext().ensureActive()
            read(tree, resources.getValue(path)) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(it, null, options)
                require(options.outWidth > 0 && options.outHeight > 0) { "无法识别图片：$path" }
            }
        }
        audio.forEach { path ->
            currentCoroutineContext().ensureActive()
            validateAudio(path, DocumentsContract.buildDocumentUriUsingTree(tree, resources.getValue(path).id))
        }

        val id = UUID.randomUUID().toString()
        val directory = File(root, id)
        check(directory.mkdirs()) { "无法创建书籍存储目录" }
        var saved = false
        try {
            File(directory, catalog.name).writeText(json)
            for ((path, entry) in resources) {
                currentCoroutineContext().ensureActive()
                val destination = File(directory, path)
                destination.parentFile?.mkdirs()
                read(tree, entry) { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    }
                }
            }
            mutations.withLock {
                checkUnique(title)
                val existing = _books.value.firstOrNull { it.source == source }
                val book = Book(id, source, directory, title, cover, units)
                val updated = if (existing == null) _books.value + book else
                    _books.value.map { if (it.id == existing.id) book else it }
                withContext(NonCancellable) {
                    existing?.let { release(it.id) }
                    save(updated)
                    saved = true
                    existing?.directory?.deleteRecursively()
                }
            }
        } finally {
            if (!saved) directory.deleteRecursively()
        }
    }

    suspend fun delete(id: String, release: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        mutations.withLock {
            loadLocked()
            val book = _books.value.firstOrNull { it.id == id } ?: return@withLock
            withContext(NonCancellable) {
                release(id)
                save(_books.value.filterNot { it.id == id })
                check(book.directory.deleteRecursively()) { "无法完全清理书籍文件：${book.title}" }
            }
        }
    }

    private fun loadLocked() {
        if (loaded) return
        val records = try {
            JSONArray(String(index.readFully(), Charsets.UTF_8))
        } catch (error: FileNotFoundException) {
            if (index.baseFile.exists()) throw error
            JSONArray()
        }
        _books.value = List(records.length()) { i ->
            val record = records.getJSONObject(i)
            val id = record.getString("id")
            require(UUID.fromString(id).toString() == id) { "书库索引包含无效编号" }
            val directory = File(root, id)
            val catalog = requireNotNull(directory.listFiles()?.singleOrNull {
                it.isFile && it.extension.equals("json", true)
            }) { "书籍目录 JSON 不可用：$id" }
            val (title, cover, units) = BookParser.parse(catalog.readText())
            Book(id, record.getString("source"), directory, title, cover, units)
        }
        val indexed = _books.value.map { it.id }.toSet()
        root.listFiles()?.filter { directory ->
            directory.isDirectory && directory.name !in indexed &&
                runCatching { UUID.fromString(directory.name).toString() == directory.name }.getOrDefault(false)
        }?.forEach { it.deleteRecursively() }
        loaded = true
    }

    private fun checkUnique(title: String) {
        require(_books.value.none { it.title == title }) { "书库中已有同名书" }
    }

    private fun save(books: List<Book>) {
        val records = JSONArray()
        books.forEach { records.put(JSONObject().put("id", it.id).put("source", it.source)) }
        val output = index.startWrite()
        try {
            output.write(records.toString().toByteArray(Charsets.UTF_8))
            index.finishWrite(output)
        } catch (error: Exception) {
            index.failWrite(output)
            throw error
        }
        _books.value = books
    }

    private data class Entry(val id: String, val name: String, val directory: Boolean)

    private fun children(tree: Uri, id: String): Map<String, Entry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        val cursor = requireNotNull(resolver.query(uri, columns, null, null, null)) { "无法读取所选目录" }
        return cursor.use {
            buildMap {
                while (it.moveToNext()) {
                    val name = it.getString(1)
                    require(!name.isNullOrBlank() && '/' !in name && '\\' !in name &&
                        ':' !in name && name != "." && name != "..") { "目录中有无效文件名" }
                    val entry = Entry(it.getString(0), name, it.getString(2) == Document.MIME_TYPE_DIR)
                    require(put(name, entry) == null) { "目录中出现重复文件名：$name" }
                }
            }
        }
    }

    private inline fun <T> read(tree: Uri, entry: Entry, block: (java.io.InputStream) -> T): T {
        val input = try {
            resolver.openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree, entry.id))
        } catch (error: Exception) {
            throw IllegalArgumentException("无法打开文件：${entry.name}", error)
        }
        return requireNotNull(input) { "无法打开文件：${entry.name}" }.use(block)
    }

    private fun validateAudio(path: String, uri: Uri) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(resolverContext, uri, null)
            val audioTrack = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == "audio/mpeg"
            }
            requireNotNull(audioTrack)
            extractor.selectTrack(audioTrack)
            require(extractor.sampleSize > 0)
        } catch (error: Exception) {
            throw IllegalArgumentException("无法识别 MP3：$path", error)
        } finally {
            extractor.release()
        }
    }

    private val resolverContext = context.applicationContext
}
