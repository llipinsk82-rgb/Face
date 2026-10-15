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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import java.util.Locale

@Composable
fun VideoTimelineControls(
  videoInfo: VideoInfo?,
  currentTimeMs: Long,
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  onSeek: (Long) -> Unit,
  onLoadDemoVideo: () -> Unit,
  onPickNewVideo: () -> Unit,
  modifier: Modifier = Modifier
) {
  val durationMs = (videoInfo?.durationMs ?: 5000L).coerceAtLeast(1000L)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
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
          color = StudioCyan,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )

        Text(
          text = "Wideo: ${videoInfo?.width ?: 960}x${videoInfo?.height ?: 540} • Bez limitu długości",
          color = Color.Gray,
          fontSize = 11.sp
        )

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
          thumbColor = StudioCyan,
          activeTrackColor = StudioCyan
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
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E2838)),
          modifier = Modifier
            .size(42.dp)
            .testTag("btn_step_backward")
        ) {
          Icon(Icons.Default.FastRewind, contentDescription = "-1 sekunda", tint = Color.White)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Play / Pause button
        FilledIconButton(
          onClick = onTogglePlay,
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioCyan),
          modifier = Modifier
            .size(54.dp)
            .testTag("btn_toggle_play_pause")
        ) {
          Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Wstrzymaj" else "Odtwarzaj",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Step forward 1 sec
        FilledIconButton(
          onClick = { onSeek((currentTimeMs + 1000L).coerceAtMost(durationMs)) },
          colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E2838)),
          modifier = Modifier
            .size(42.dp)
            .testTag("btn_step_forward")
        ) {
          Icon(Icons.Default.FastForward, contentDescription = "+1 sekunda", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Switch / Load video buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onPickNewVideo,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A354A)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_pick_new_video")
        ) {
          Icon(Icons.Default.VideoFile, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Wybierz Wideo z Plików", color = Color.White, fontSize = 11.sp, maxLines = 1)
        }

        OutlinedButton(
          onClick = onLoadDemoVideo,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, StudioMint.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("btn_load_demo_video")
        ) {
          Text("Wideo Demo", color = StudioMint, fontSize = 11.sp)
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
