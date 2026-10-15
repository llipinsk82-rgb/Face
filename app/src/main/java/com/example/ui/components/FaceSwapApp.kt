package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaMode
import com.example.ui.FaceSwapViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceSwapApp(
  viewModel: FaceSwapViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // Activity Result Launchers using standard zero-permission Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadPhotoFromUri(context, uri)
    }
  }

  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadVideoFromUri(context, uri)
    }
  }

  val customFacePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.addCustomFaceFromUri(context, uri, "Moja Twarz")
    }
  }

  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.dismissToast()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = DarkBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StudioCyan),
              contentAlignment = Alignment.Center
            ) {
              Text("FS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }

            Column {
              Text(
                text = "FaceSwap Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Wideo & Zdjęcia • 100% Offline • Bez limitu",
                style = MaterialTheme.typography.bodySmall,
                color = StudioCyan,
                fontSize = 11.sp
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DarkSurface
        ),
        actions = {
          // Info chip
          Box(
            modifier = Modifier
              .padding(end = 12.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E2838))
              .border(1.dp, StudioMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "PRO FREE",
              color = StudioMint,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      )
    },
    bottomBar = {
      // Bottom sticky action bar
      Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Change source media button
          OutlinedButton(
            onClick = {
              if (uiState.mediaMode == MediaMode.PHOTO) {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              } else {
                videoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_change_source_media")
          ) {
            Icon(
              Icons.Default.AddPhotoAlternate,
              contentDescription = null,
              tint = StudioCyan,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.mediaMode == MediaMode.PHOTO) "Zmień Zdjęcie" else "Zmień Wideo",
              color = Color.White,
              fontSize = 12.sp,
              maxLines = 1
            )
          }

          // Save / Export button
          Button(
            onClick = { viewModel.exportCurrentMedia(context) },
            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_export_media")
          ) {
            Icon(
              Icons.Default.Download,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Zapisz Klatkę",
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              maxLines = 1
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
    ) {
      // Mode Switcher Tabs
      TabRow(
        selectedTabIndex = if (uiState.mediaMode == MediaMode.PHOTO) 0 else 1,
        containerColor = DarkSurface,
        contentColor = StudioCyan,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(
              tabPositions[if (uiState.mediaMode == MediaMode.PHOTO) 0 else 1]
            ),
            color = StudioCyan
          )
        }
      ) {
        Tab(
          selected = uiState.mediaMode == MediaMode.PHOTO,
          onClick = { viewModel.setMediaMode(MediaMode.PHOTO) },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("ZDJĘCIA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          },
          modifier = Modifier.testTag("tab_photos")
        )

        Tab(
          selected = uiState.mediaMode == MediaMode.VIDEO,
          onClick = { viewModel.setMediaMode(MediaMode.VIDEO) },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("WIDEO", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          },
          modifier = Modifier.testTag("tab_videos")
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Media Preview Card
      val activeRendered = if (uiState.mediaMode == MediaMode.PHOTO) {
        uiState.renderedPhoto
      } else {
        uiState.renderedVideoFrame
      }

      val activeOriginal = if (uiState.mediaMode == MediaMode.PHOTO) {
        uiState.originalPhoto
      } else {
        uiState.currentVideoFrame
      }

      val activeDetectedFaces = if (uiState.mediaMode == MediaMode.PHOTO) {
        uiState.detectedPhotoFaces
      } else {
        uiState.detectedVideoFaces
      }

      val activeLoading = if (uiState.mediaMode == MediaMode.PHOTO) {
        uiState.isDetectingPhoto
      } else {
        uiState.isExtractingVideoFrame
      }

      MediaPreviewCard(
        renderedBitmap = activeRendered,
        originalBitmap = activeOriginal,
        detectedFaces = activeDetectedFaces,
        selectedFaceId = uiState.selectedFaceIdToMap,
        isLoading = activeLoading,
        onFaceTapped = { faceId ->
          viewModel.selectFaceForMapping(faceId)
        }
      )

      // Reset / Sample helper buttons under preview
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (uiState.mediaMode == MediaMode.PHOTO) {
          OutlinedButton(
            onClick = { viewModel.loadSamplePhoto() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_load_sample_photo")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Wczytaj Zdjęcie Próbne (3 osoby)", fontSize = 11.sp, color = StudioCyan)
          }
        } else {
          OutlinedButton(
            onClick = { viewModel.loadDemoVideo() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_reset_demo_video")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Wczytaj Klip Demo", fontSize = 11.sp, color = StudioMint)
          }
        }

        Text(
          text = "Dotknij twarzy aby wybrać",
          fontSize = 11.sp,
          color = Color.Gray
        )
      }

      // Video Timeline Controls (only in Video mode)
      if (uiState.mediaMode == MediaMode.VIDEO) {
        VideoTimelineControls(
          videoInfo = uiState.videoInfo,
          currentTimeMs = uiState.currentVideoTimeMs,
          isPlaying = uiState.isPlayingVideo,
          onTogglePlay = { viewModel.toggleVideoPlayback(context) },
          onSeek = { ms -> viewModel.seekVideo(context, ms) },
          onLoadDemoVideo = { viewModel.loadDemoVideo() },
          onPickNewVideo = {
            videoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          }
        )
      }

      // Face Selection & Mapping Bar
      FaceSelectionBar(
        detectedFaces = activeDetectedFaces,
        availableFaces = uiState.allAvailableFaces,
        swapConfig = uiState.swapConfig,
        selectedFaceId = uiState.selectedFaceIdToMap,
        onSelectFaceToMap = { faceId -> viewModel.selectFaceForMapping(faceId) },
        onModeChanged = { singleForAll -> viewModel.setSwapMode(singleForAll) },
        onChooseFaceForCurrent = { face ->
          if (uiState.swapConfig.isSingleForAll) {
            viewModel.setGlobalReplacementFace(face)
          } else {
            viewModel.setIndividualFaceMapping(uiState.selectedFaceIdToMap, face)
          }
        },
        onAddNewFaceClick = {
          customFacePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        }
      )

      // Fine Tuning Controls
      TuningControlsCard(
        config = uiState.swapConfig,
        onScaleChange = { viewModel.updateScale(it) },
        onYOffsetChange = { viewModel.updateYOffset(it) },
        onAlphaChange = { viewModel.updateAlpha(it) },
        onToggleRotation = { viewModel.toggleRotation(it) },
        onToggleOvalMask = { viewModel.toggleOvalMask(it) }
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
