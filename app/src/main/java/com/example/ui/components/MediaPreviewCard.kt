package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
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
import com.example.model.DetectedFaceInfo
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioViolet

@Composable
fun MediaPreviewCard(
  renderedBitmap: Bitmap?,
  originalBitmap: Bitmap?,
  detectedFaces: List<DetectedFaceInfo>,
  selectedFaceId: Int,
  isLoading: Boolean,
  onFaceTapped: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var showBoundingBoxes by remember { mutableStateOf(false) }
  var showOriginal by remember { mutableStateOf(false) }

  val activeBitmap = if (showOriginal) originalBitmap else renderedBitmap ?: originalBitmap

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("media_preview_card"),
    shape = RoundedCornerShape(20.dp),
    color = DarkSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 10f)
        .background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
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
                    // Map tap offset to bitmap coordinate
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val bmpW = activeBitmap.width.toFloat()
                    val bmpH = activeBitmap.height.toFloat()

                    // Compute aspect fit scale and letterbox offset
                    val scale = (canvasW / bmpW).coerceAtMost(canvasH / bmpH)
                    val fitW = bmpW * scale
                    val fitH = bmpH * scale
                    val offX = (canvasW - fitW) / 2f
                    val offY = (canvasH - fitH) / 2f

                    if (tapOffset.x in offX..(offX + fitW) && tapOffset.y in offY..(offY + fitH)) {
                      val bmpX = (tapOffset.x - offX) / scale
                      val bmpY = (tapOffset.y - offY) / scale

                      // Find which face bounds contains this point
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
                  color = if (isSelected) StudioViolet else StudioCyan,
                  topLeft = Offset(boxLeft, boxTop),
                  size = Size(boxWidth, boxHeight),
                  cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                  style = Stroke(width = if (isSelected) 5f else 3f)
                )
              }
            }
          }
        }
      } else {
        Text(
          text = "Wczytaj zdjęcie lub wideo",
          color = Color.Gray,
          fontSize = 14.sp
        )
      }

      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = StudioCyan,
            modifier = Modifier.size(36.dp)
          )
        }
      }

      // Top floating status & controls badges
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, StudioCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Wykryto: ${detectedFaces.size} twarzy",
            color = StudioCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Toggle Bounding Boxes button
        IconButton(
          onClick = { showBoundingBoxes = !showBoundingBoxes },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("btn_toggle_bounding_boxes")
        ) {
          Icon(
            Icons.Default.CropFree,
            contentDescription = "Pokaż ramki twarzy",
            tint = if (showBoundingBoxes) StudioCyan else Color.LightGray,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Hold / Click to peek original photo
        IconButton(
          onClick = { showOriginal = !showOriginal },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("btn_toggle_original")
        ) {
          Icon(
            if (showOriginal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = "Porównaj z oryginałem",
            tint = if (showOriginal) StudioViolet else Color.LightGray,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
