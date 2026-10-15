package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetectedFaceInfo
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSecondary

@Composable
fun FaceSelectionBar(
  detectedFaces: List<DetectedFaceInfo>,
  availableFaces: List<ReplacementFace>,
  swapConfig: SwapConfig,
  selectedFaceId: Int,
  onSelectFaceToMap: (Int) -> Unit,
  onModeChanged: (Boolean) -> Unit,
  onChooseFaceForCurrent: (ReplacementFace) -> Unit,
  onAddNewFaceClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("face_selection_bar"),
    shape = RoundedCornerShape(16.dp),
    color = DarkSurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
  ) {
    Column(
      modifier = Modifier.padding(14.dp)
    ) {
      // Mode selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Tryb podmiany:",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = swapConfig.isSingleForAll,
            onClick = { onModeChanged(true) },
            label = { Text("Wszyscy", fontSize = 12.sp) },
            leadingIcon = {
              Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioPrimary,
              selectedLabelColor = Color(0xFF082F49)
            ),
            modifier = Modifier.testTag("mode_all_faces_button")
          )

          FilterChip(
            selected = !swapConfig.isSingleForAll,
            onClick = { onModeChanged(false) },
            label = { Text("Osobno", fontSize = 12.sp) },
            leadingIcon = {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioSecondary,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("mode_individual_faces_button")
          )
        }
      }

      // If Individual mode: Show row of detected people in media
      if (!swapConfig.isSingleForAll && detectedFaces.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Wybierz osobę do podmiany (${detectedFaces.size} wykrytych):",
          fontSize = 12.sp,
          color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(vertical = 4.dp)
        ) {
          items(detectedFaces) { face ->
            val isSelected = face.id == selectedFaceId
            val assignedFace = swapConfig.individualMappings[face.id] ?: swapConfig.globalReplacement

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectFaceToMap(face.id) }
                .background(if (isSelected) StudioSecondary.copy(alpha = 0.2f) else Color.Transparent)
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) StudioSecondary else DarkBorder,
                  shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
                .testTag("detected_person_${face.id}")
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Color.Black),
                contentAlignment = Alignment.Center
              ) {
                if (face.faceThumbnail != null) {
                  Image(
                    bitmap = face.faceThumbnail.asImageBitmap(),
                    contentDescription = "Osoba ${face.id + 1}",
                    modifier = Modifier.size(46.dp)
                  )
                } else {
                  Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = StudioPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Osoba ${face.id + 1}",
                fontSize = 11.sp,
                color = if (isSelected) StudioSecondary else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )

              if (assignedFace != null) {
                Text(
                  text = assignedFace.name,
                  fontSize = 10.sp,
                  color = StudioPrimary,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Available Replacement Faces Carousel
      Text(
        text = if (swapConfig.isSingleForAll) {
          "Wybierz maskę / twarz:"
        } else {
          "Maska dla osoby ${selectedFaceId + 1}:"
        },
        fontSize = 12.sp,
        color = Color.LightGray
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
      ) {
        // "Add Custom Face" Button
        item {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { onAddNewFaceClick() }
              .background(Color(0xFF1E293B))
              .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
              .padding(8.dp)
              .testTag("add_custom_face_button")
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(StudioPrimary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Add,
                contentDescription = "Dodaj twarz ze zdjęcia",
                tint = StudioPrimary,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "+ Własna",
              fontSize = 11.sp,
              color = StudioPrimary,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Available faces
        items(availableFaces) { face ->
          val currentActive = if (swapConfig.isSingleForAll) {
            swapConfig.globalReplacement?.id == face.id
          } else {
            (swapConfig.individualMappings[selectedFaceId]?.id ?: swapConfig.globalReplacement?.id) == face.id
          }

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { onChooseFaceForCurrent(face) }
              .background(if (currentActive) StudioPrimary.copy(alpha = 0.15f) else Color.Transparent)
              .border(
                width = if (currentActive) 2.dp else 1.dp,
                color = if (currentActive) StudioPrimary else DarkBorder,
                shape = RoundedCornerShape(12.dp)
              )
              .padding(8.dp)
              .testTag("replacement_face_${face.id}")
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black),
              contentAlignment = Alignment.Center
            ) {
              Image(
                bitmap = face.bitmap.asImageBitmap(),
                contentDescription = face.name,
                modifier = Modifier.size(48.dp)
              )

              if (currentActive) {
                Box(
                  modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StudioPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = face.name,
              fontSize = 11.sp,
              color = if (currentActive) StudioPrimary else Color.White,
              fontWeight = if (currentActive) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
    }
  }
}
