package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun TrackArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48
) {
    val sizeModifier = modifier
        .size(sizeDp.dp)
        .clip(RoundedCornerShape(8.dp))

    if (artworkUri.isNullOrBlank()) {
        ArtworkFallback(sizeModifier)
        return
    }

    SubcomposeAsyncImage(
        model = artworkUri,
        contentDescription = "Track artwork",
        contentScale = ContentScale.Crop,
        modifier = sizeModifier,
    ) {
        when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> ArtworkFallback(sizeModifier)
        }
    }
}

@Composable
private fun ArtworkFallback(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
