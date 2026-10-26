package com.example.musicplayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val exo: ExoPlayer = ExoPlayer.Builder(app).build()

    var songs by mutableStateOf<List<Song>>(emptyList())
    var current by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsStatus by mutableStateOf("")

    private var queue: List<Song> = emptyList()

    init {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) next()
            }
        })

        viewModelScope.launch {
            while (true) {
                positionMs = exo.currentPosition
                durationMs = exo.duration.coerceAtLeast(0L)
                isPlaying = exo.isPlaying
                delay(250)
            }
        }
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            songs = withContext(Dispatchers.IO) { LocalMusic.load(getApplication()) }
        }
    }

    fun play(song: Song, playlist: List<Song> = listOf(song)) {
        queue = playlist
        current = song
        exo.setMediaItem(MediaItem.fromUri(song.uri))
        exo.prepare()
        exo.play()
        loadLyrics(song)
    }

    private fun loadLyrics(song: Song) {
        lyrics = emptyList()
        lyricsStatus = "Loading lyrics..."
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching { Lyrics.get(getApplication(), song.title, song.artist) }
                    .getOrDefault(emptyList())
            }
            if (current?.uri == song.uri) {
                lyrics = found
                lyricsStatus = if (found.isEmpty()) "No synced lyrics found for this song" else ""
            }
        }
    }

    fun togglePlay() {
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun seekTo(ms: Long) {
        exo.seekTo(ms)
    }

    fun next() {
        val index = queue.indexOfFirst { it.uri == current?.uri }
        if (index in 0 until queue.size - 1) play(queue[index + 1], queue)
    }

    fun previous() {
        val index = queue.indexOfFirst { it.uri == current?.uri }
        if (index > 0) play(queue[index - 1], queue)
    }

    override fun onCleared() {
        exo.release()
        super.onCleared()
    }
}
