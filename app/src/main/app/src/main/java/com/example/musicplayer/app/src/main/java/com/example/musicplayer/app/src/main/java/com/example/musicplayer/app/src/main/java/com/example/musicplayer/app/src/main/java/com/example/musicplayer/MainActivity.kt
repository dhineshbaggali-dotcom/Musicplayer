package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen(vm: PlayerViewModel = viewModel()) {
    val context = LocalContext.current

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.loadLocalSongs()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            vm.loadLocalSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Text(
            "My Music",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Box(Modifier.weight(1f)) {
            LocalSongList(vm)
        }

        if (vm.current != null) NowPlaying(vm)
    }
}

@Composable
fun LocalSongList(vm: PlayerViewModel) {
    if (vm.songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No songs found on this device yet.")
        }
        return
    }
    LazyColumn {
        items(vm.songs) { song ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.play(song, vm.songs) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(song.artist, maxLines = 1)
            }
        }
    }
}

@Composable
fun NowPlaying(vm: PlayerViewModel) {
    val song = vm.current ?: return
    val listState = rememberLazyListState()

    val activeIndex = vm.lyrics.indexOfLast { it.timeMs <= vm.positionMs }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }

    val duration = vm.durationMs.coerceAtLeast(1L).toFloat()
    val position = vm.positionMs.coerceIn(0L, vm.durationMs.coerceAtLeast(1L)).toFloat()

    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(song.artist, maxLines = 1)

        Slider(
            value = position,
            onValueChange = { vm.seekTo(it.toLong()) },
            valueRange = 0f..duration
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { vm.previous() }) { Text("Prev") }
            Button(onClick = { vm.togglePlay() }) {
                Text(if (vm.isPlaying) "Pause" else "Play")
            }
            Button(onClick = { vm.next() }) { Text("Next") }
        }

        Box(Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp)) {
            if (vm.lyrics.isEmpty()) {
                Text(vm.lyricsStatus, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(state = listState) {
                    itemsIndexed(vm.lyrics) { index, line ->
                        val active = index == activeIndex
                        Text(
                            text = line.text,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
