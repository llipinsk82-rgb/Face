package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoInfo
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.StudioPrimary
import java.util.Locale

@Composable
fun VideoTimelineControls(
  videoInfo: VideoInfo?,
  currentTimeMs: Long,
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  onSeek: (Long) -> Unit,
  onPickGalleryVideo: () -> Unit,
  onPickSafVideo: () -> Unit,
  modifier: Modifier = Modifier
) {
  val durationMs = (videoInfo?.durationMs ?: 0L).coerceAtLeast(1L)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("video_timeline_controls"),
    shape = RoundedCornerShape(16.dp),
    color = DarkSurfaceCard,
    border = BorderStroke(1.dp, DarkBorder)
  ) {
    Column(
      modifier = Modifier.padding(14.dp)
    ) {
      // Timeline Scrubber & Times
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formatTimestamp(currentTimeMs),
          color = StudioPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )

        if (videoInfo != null) {
          Text(
            text = "${videoInfo.width}x${videoInfo.height} • ${if (videoInfo.isVertical) "Pionowe" else "Poziome"}${if (videoInfo.hasAudio) " • Dźwięk" else ""}",
            color = Color.Gray,
            fontSize = 11.sp
          )
        }

        Text(
          text = formatTimestamp(durationMs),
          color = Color.LightGray,
          fontSize = 13.sp
        )
      }

      Slider(
        value = (currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
        onValueChange = { fraction ->
          onSeek((fraction * durationMs).toLong())
        },
        colors = SliderDefaults.colors(
          thumbColor = StudioPrimary,
          activeTrackColor = StudioPrimary
        ),
        modifier = Modifier.testTag("video_timeline_slider")
      )

      // Playback Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Step back 1 sec
        FilledIconButton(
          onClick = { onSeek((currentTimeMs - 1000L).coerceAtLeast(0L)) },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier
            .size(42.dp)
            .testTag("btn_step_backward")
        ) {
          Icon(Icons.Default.FastRewind, contentDescription = "-1 sekunda", tint = Color.White)
        }

        Spacer(modifier = Modifier.width(18.dp))

        // Play / Pause button
        FilledIconButton(
          onClick = onTogglePlay,
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioPrimary),
          modifier = Modifier
            .size(52.dp)
            .testTag("btn_toggle_play_pause")
        ) {
          Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Wstrzymaj" else "Odtwarzaj",
            tint = Color(0xFF082F49),
            modifier = Modifier.size(30.dp)
          )
        }

        Spacer(modifier = Modifier.width(18.dp))

        // Step forward 1 sec
        FilledIconButton(
          onClick = { onSeek((currentTimeMs + 1000L).coerceAtMost(durationMs)) },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier
            .size(42.dp)
            .testTag("btn_step_forward")
        ) {
          Icon(Icons.Default.FastForward, contentDescription = "+1 sekunda", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Video Source Chooser Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onPickGalleryVideo,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_pick_gallery_video")
        ) {
          Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Galeria Wideo", color = Color.White, fontSize = 11.sp, maxLines = 1)
        }

        OutlinedButton(
          onClick = onPickSafVideo,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color(0xFF334155)),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_pick_saf_video")
        ) {
          Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pliki urządzenia", color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
        }
      }
    }
  }
}

private fun formatTimestamp(ms: Long): String {
  val totalSeconds = ms / 1000
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
