package com.kid.audioplayer.data

import android.util.JsonReader
import android.util.JsonToken
import java.io.IOException
import java.io.StringReader
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToLong

internal data class BookContents(val title: String, val cover: String, val units: List<BookUnit>)

internal object BookParser {
    fun parse(json: String): BookContents {
        val root = try {
            JsonReader(StringReader(json)).use { reader ->
                val value = reader.readValue(0) as? JSONObject
                require(value != null && reader.peek() == JsonToken.END_DOCUMENT) { "目录 JSON 必须是一个对象" }
                value
            }
        } catch (error: IOException) {
            throw IllegalArgumentException("目录 JSON 无效", error)
        }
        val title = root.requiredText("title", "title").trim()
        val cover = path(root.requiredText("picture", "picture"), "picture")
        val units = root.optJSONArray("units")
        require(units != null && units.length() > 0) { "units 必须是非空数组" }
        return BookContents(title, cover, List(units.length()) { index ->
            val field = "units[$index]"
            val unit = requireNotNull(units.optJSONObject(index)) { "$field 必须是对象" }
            val unitTitle = unit.requiredText("title", "$field.title").trim()
            val audio = path(unit.requiredText("audio", "$field.audio"), "$field.audio")
            require(audio.endsWith(".mp3", ignoreCase = true)) { "$field.audio 必须引用 MP3" }
            val times = unit.optJSONArray("time")
            val images = unit.optJSONArray("picture")
            require(times != null && images != null && times.length() > 0 &&
                times.length() == images.length()) { "$field.time 与 $field.picture 必须逐项配对" }
            var previousEnd = 0.0
            val pages = List(times.length()) { page ->
                val timeField = "$field.time[$page]"
                val time = requireNotNull(times.optJSONObject(page)) { "$timeField 必须是对象" }
                val start = time.seconds("start", timeField)
                val end = time.seconds("end", timeField)
                val startMs = (start * 1000).roundToLong()
                val endMs = (end * 1000).roundToLong()
                require(start >= previousEnd && endMs > startMs) { "$timeField 时间无效或与前一段重叠" }
                previousEnd = end
                val imageField = "$field.picture[$page]"
                val image = (images.opt(page) as? String).orEmpty()
                BookPage(path(image, imageField), startMs, endMs)
            }
            BookUnit(unitTitle, audio, pages)
        })
    }

    private fun JsonReader.readValue(depth: Int): Any {
        require(depth <= 32) { "目录 JSON 嵌套过深" }
        return when (peek()) {
            JsonToken.BEGIN_OBJECT -> JSONObject().also { value ->
                beginObject()
                while (hasNext()) {
                    val key = nextName()
                    require(!value.has(key)) { "目录 JSON 中出现重复字段：$key" }
                    value.put(key, readValue(depth + 1))
                }
                endObject()
            }
            JsonToken.BEGIN_ARRAY -> JSONArray().also { value ->
                beginArray()
                while (hasNext()) value.put(readValue(depth + 1))
                endArray()
            }
            JsonToken.STRING -> nextString()
            JsonToken.NUMBER -> nextDouble()
            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.NULL -> JSONObject.NULL.also { nextNull() }
            else -> throw IllegalArgumentException("目录 JSON 无效")
        }
    }

    private fun JSONObject.requiredText(key: String, field: String): String =
        (opt(key) as? String)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$field 必须是非空文本")

    private fun JSONObject.seconds(key: String, field: String): Double {
        val seconds = (opt(key) as? Number)?.toDouble() ?: Double.NaN
        require(seconds.isFinite() && seconds >= 0 && seconds * 1000 < Long.MAX_VALUE.toDouble()) {
            "$field.$key 必须是非负秒数"
        }
        return seconds
    }

    private fun path(value: String, field: String): String {
        require(value.isNotBlank() && '\\' !in value && ':' !in value &&
            value.none { it.isISOControl() } && value.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "$field 必须是书籍目录内的相对路径"
        }
        return value
    }
}
