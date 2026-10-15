package com.example.ui.components

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.DetectedFaceInfo
import com.example.model.MediaMode
import com.example.model.VideoInfo
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary

@OptIn(UnstableApi::class)
@Composable
fun MediaPreviewCard(
  mediaMode: MediaMode,
  renderedBitmap: Bitmap?,
  originalBitmap: Bitmap?,
  detectedFaces: List<DetectedFaceInfo>,
  selectedFaceId: Int,
  isLoading: Boolean,
  isPlayingVideo: Boolean,
  exoPlayer: ExoPlayer?,
  videoInfo: VideoInfo?,
  showBeforeAfterComparison: Boolean,
  onFaceTapped: (Int) -> Unit,
  onToggleComparison: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var showBoundingBoxes by remember { mutableStateOf(false) }

  // Target aspect ratio: use video's real aspect ratio if in video mode, else default 4:3 or bitmap ratio
  val targetAspectRatio = if (mediaMode == MediaMode.VIDEO && videoInfo != null && videoInfo.aspectRatio > 0f) {
    videoInfo.aspectRatio.coerceIn(0.5f, 2.2f)
  } else if (renderedBitmap != null && renderedBitmap.height > 0) {
    (renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat()).coerceIn(0.5f, 2.2f)
  } else {
    16f / 9f
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("media_preview_card"),
    shape = RoundedCornerShape(16.dp),
    color = DarkSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 220.dp, max = 500.dp)
        .aspectRatio(targetAspectRatio)
        .background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
      if (mediaMode == MediaMode.VIDEO && isPlayingVideo && exoPlayer != null) {
        // Real ExoPlayer Video Playback with real audio
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = exoPlayer
              useController = false
              resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
              setBackgroundColor(android.graphics.Color.BLACK)
            }
          },
          update = { playerView ->
            playerView.player = exoPlayer
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        // Still frame / Photo rendering (when paused, scrubbing, or viewing photo)
        val activeBitmap = if (showBeforeAfterComparison) originalBitmap else (renderedBitmap ?: originalBitmap)

        if (activeBitmap != null) {
          val imageBitmap = remember(activeBitmap) { activeBitmap.asImageBitmap() }

          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              bitmap = imageBitmap,
              contentDescription = "Podgląd z podmienionymi twarzami",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxSize()
                .pointerInput(detectedFaces, activeBitmap) {
                  detectTapGestures { tapOffset ->
                    if (detectedFaces.isNotEmpty()) {
                      val canvasW = size.width.toFloat()
                      val canvasH = size.height.toFloat()
                      val bmpW = activeBitmap.width.toFloat()
                      val bmpH = activeBitmap.height.toFloat()

                      val scale = (canvasW / bmpW).coerceAtMost(canvasH / bmpH)
                      val fitW = bmpW * scale
                      val fitH = bmpH * scale
                      val offX = (canvasW - fitW) / 2f
                      val offY = (canvasH - fitH) / 2f

                      if (tapOffset.x in offX..(offX + fitW) && tapOffset.y in offY..(offY + fitH)) {
                        val bmpX = (tapOffset.x - offX) / scale
                        val bmpY = (tapOffset.y - offY) / scale

                        val tapped = detectedFaces.firstOrNull { face ->
                          face.bounds.contains(bmpX.toInt(), bmpY.toInt())
                        }
                        if (tapped != null) {
                          onFaceTapped(tapped.id)
                        }
                      }
                    }
                  }
                }
            )

            // Bounding boxes overlay if enabled
            if (showBoundingBoxes && detectedFaces.isNotEmpty()) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val bmpW = activeBitmap.width.toFloat()
                val bmpH = activeBitmap.height.toFloat()

                val scale = (canvasW / bmpW).coerceAtMost(canvasH / bmpH)
                val fitW = bmpW * scale
                val fitH = bmpH * scale
                val offX = (canvasW - fitW) / 2f
                val offY = (canvasH - fitH) / 2f

                detectedFaces.forEach { face ->
                  val isSelected = face.id == selectedFaceId
                  val boxLeft = offX + (face.bounds.left * scale)
                  val boxTop = offY + (face.bounds.top * scale)
                  val boxWidth = face.bounds.width() * scale
                  val boxHeight = face.bounds.height() * scale

                  drawRoundRect(
                    color = if (isSelected) StudioSecondary else StudioPrimary,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = if (isSelected) 4f else 2.5f)
                  )
                }
              }
            }
          }
        } else {
          Text(
            text = if (mediaMode == MediaMode.VIDEO) "Wybierz plik wideo do odtworzenia" else "Wybierz zdjęcie do edycji",
            color = Color.Gray,
            fontSize = 13.sp
          )
        }
      }

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = StudioPrimary,
            modifier = Modifier.size(36.dp)
          )
        }
      }

      // Top floating status badges
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Face Count Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (detectedFaces.isEmpty()) "Brak wykrytych twarzy" else "Wykryto: ${detectedFaces.size} twarzy",
            color = if (detectedFaces.isEmpty()) Color.LightGray else StudioPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Toggle Bounding Boxes
        IconButton(
          onClick = { showBoundingBoxes = !showBoundingBoxes },
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.75f))
            .testTag("btn_toggle_bounding_boxes")
        ) {
          Icon(
            Icons.Default.CropFree,
            contentDescription = "Pokaż ramki twarzy",
            tint = if (showBoundingBoxes) StudioPrimary else Color.LightGray,
            modifier = Modifier.size(17.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Przed / Po (Before / After Comparison)
        IconButton(
          onClick = { onToggleComparison(!showBeforeAfterComparison) },
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (showBeforeAfterComparison) StudioSecondary else Color.Black.copy(alpha = 0.75f))
            .testTag("btn_toggle_original")
        ) {
          Icon(
            if (showBeforeAfterComparison) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = "Porównaj Przed/Po",
            tint = if (showBeforeAfterComparison) Color.White else Color.LightGray,
            modifier = Modifier.size(17.dp)
          )
        }
      }

      // Floating "Przed / Po" badge when comparison is active
      if (showBeforeAfterComparison) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSecondary.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
          Text(
            text = "ORYGINAŁ (PRZED)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}
