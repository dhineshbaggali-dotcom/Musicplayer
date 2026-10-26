package com.example.musicplayer

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

object Lyrics {
    private val client = OkHttpClient()

    fun get(context: Context, title: String, artist: String): List<LyricLine> {
        val folder = File(context.filesDir, "lyrics").apply { mkdirs() }
        val cacheFile = File(folder, safeName("$artist - $title") + ".lrc")

        if (cacheFile.exists()) {
            return parseLrc(cacheFile.readText())
        }

        val url = "https://lrclib.net/api/search" +
                "?track_name=${encode(title)}&artist_name=${encode(artist)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MusicPlayerApp/1.0")
            .build()

        val body = client.newCall(request).execute().use { it.body.string() }
        val results = JSONArray(body)

        for (i in 0 until results.length()) {
            val synced = results.getJSONObject(i).optString("syncedLyrics")
            if (synced.isNotBlank()) {
                cacheFile.writeText(synced)
                return parseLrc(synced)
            }
        }
        return emptyList()
    }

    fun parseLrc(text: String): List<LyricLine> {
        val pattern = Regex("""\[(\d+):(\d+)(?:\.(\d+))?](.*)""")
        return text.lines()
            .mapNotNull { line ->
                pattern.find(line.trim())?.let { match ->
                    val minutes = match.groupValues[1].toLong()
                    val seconds = match.groupValues[2].toLong()
                    val fraction = match.groupValues[3]
                    val millis = if (fraction.isEmpty()) 0L
                    else fraction.padEnd(3, '0').take(3).toLong()
                    LyricLine(
                        timeMs = minutes * 60_000 + seconds * 1_000 + millis,
                        text = match.groupValues[4].trim()
                    )
                }
            }
            .sortedBy { it.timeMs }
    }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")

    private fun safeName(text: String): String =
        text.replace(Regex("[^A-Za-z0-9 _-]"), "_")
}
