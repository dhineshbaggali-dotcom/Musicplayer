package com.example.musicplayer

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object Jamendo {
    // Paste your Jamendo Client ID between the quotes below
    private const val CLIENT_ID = "PASTE_YOUR_JAMENDO_CLIENT_ID_HERE"

    private val client = OkHttpClient()

    fun search(query: String): List<Song> {
        val url = "https://api.jamendo.com/v3.0/tracks/" +
                "?client_id=$CLIENT_ID" +
                "&format=json" +
                "&limit=25" +
                "&audioformat=mp32" +
                "&namesearch=${URLEncoder.encode(query, "UTF-8")}"

        val request = Request.Builder().url(url).build()
        val body = client.newCall(request).execute().use { it.body.string() }

        val results = JSONObject(body).getJSONArray("results")
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            Song(
                id = item.getString("id"),
                title = item.getString("name"),
                artist = item.getString("artist_name"),
                uri = item.getString("audio"),
                durationMs = item.optLong("duration") * 1000,
                isOnline = true,
                downloadUrl = item.optString("audiodownload").ifBlank { null }
            )
        }
    }
}
