package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
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
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceSwapApp(
  viewModel: FaceSwapViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // 1. Photo Picker (Gallery)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadPhotoFromUri(context, uri)
    }
  }

  // 2. Video Picker (Gallery)
  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadVideoFromUri(context, uri)
    }
  }

  // 3. System SAF Document Picker (fallback for any file manager or SD card)
  val safVideoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadVideoFromUri(context, uri)
    }
  }

  val safPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.loadPhotoFromUri(context, uri)
    }
  }

  // 4. Custom Replacement Face Picker
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
                .size(34.dp)
                .clip(CircleShape)
                .background(StudioPrimary),
              contentAlignment = Alignment.Center
            ) {
              Text("FS", color = Color(0xFF082F49), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Column {
              Text(
                text = "FaceSwap Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Lokalne przetwarzanie na urządzeniu (ML Kit)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DarkSurface
        )
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
          // Source selector button
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
              tint = StudioPrimary,
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

          // Real MP4 Video Export or Photo Save
          if (uiState.mediaMode == MediaMode.VIDEO) {
            Button(
              onClick = { viewModel.exportFullVideo(context) },
              colors = ButtonDefaults.buttonColors(containerColor = StudioSuccess),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1.3f)
                .testTag("btn_export_video_mp4")
            ) {
              Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Eksportuj Wideo MP4",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          } else {
            Button(
              onClick = { viewModel.exportCurrentPhoto(context) },
              colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1.2f)
                .testTag("btn_export_photo")
            ) {
              Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = Color(0xFF082F49),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Zapisz Zdjęcie",
                color = Color(0xFF082F49),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
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
      // Linear Workflow Steps Indicator
      Surface(
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("1. Wybierz plik", fontSize = 11.sp, color = StudioPrimary, fontWeight = FontWeight.SemiBold)
          Text("→", fontSize = 11.sp, color = Color.Gray)
          Text("2. Wykryj twarze", fontSize = 11.sp, color = Color.White)
          Text("→", fontSize = 11.sp, color = Color.Gray)
          Text("3. Wybierz podmianę", fontSize = 11.sp, color = Color.White)
          Text("→", fontSize = 11.sp, color = Color.Gray)
          Text("4. Eksport MP4", fontSize = 11.sp, color = StudioSuccess, fontWeight = FontWeight.SemiBold)
        }
      }

      // Mode Switcher Tabs (Wideo vs Zdjęcia)
      TabRow(
        selectedTabIndex = if (uiState.mediaMode == MediaMode.VIDEO) 0 else 1,
        containerColor = DarkSurface,
        contentColor = StudioPrimary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(
              tabPositions[if (uiState.mediaMode == MediaMode.VIDEO) 0 else 1]
            ),
            color = StudioPrimary
          )
        }
      ) {
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
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Media Preview Card (Real ExoPlayer in Video mode, Real ML Kit in Photo mode)
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
        mediaMode = uiState.mediaMode,
        renderedBitmap = activeRendered,
        originalBitmap = activeOriginal,
        detectedFaces = activeDetectedFaces,
        selectedFaceId = uiState.selectedFaceIdToMap,
        isLoading = activeLoading,
        isPlayingVideo = uiState.isPlayingVideo,
        exoPlayer = viewModel.exoPlayer,
        videoInfo = uiState.videoInfo,
        showBeforeAfterComparison = uiState.showOriginalComparison,
        onFaceTapped = { faceId -> viewModel.selectFaceForMapping(faceId) },
        onToggleComparison = { showOrig -> viewModel.toggleComparison(showOrig) }
      )

      // Video Timeline Controls (only in Video mode)
      if (uiState.mediaMode == MediaMode.VIDEO) {
        VideoTimelineControls(
          videoInfo = uiState.videoInfo,
          currentTimeMs = uiState.currentVideoTimeMs,
          isPlaying = uiState.isPlayingVideo,
          onTogglePlay = { viewModel.toggleVideoPlayback() },
          onSeek = { ms -> viewModel.seekVideo(ms) },
          onPickGalleryVideo = {
            videoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          },
          onPickSafVideo = {
            safVideoPickerLauncher.launch(arrayOf("video/*"))
          }
        )
      } else {
        // Photo mode file options
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("btn_photo_gallery")
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Galeria zdjęć", fontSize = 11.sp, color = StudioPrimary)
          }

          OutlinedButton(
            onClick = {
              safPhotoPickerLauncher.launch(arrayOf("image/*"))
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("btn_photo_saf")
          ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pliki urządzenia", fontSize = 11.sp, color = Color.LightGray)
          }
        }
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

      // Fine Tuning Controls (Advanced, collapsed by default)
      TuningControlsCard(
        config = uiState.swapConfig,
        onScaleChange = { viewModel.updateScale(it) },
        onYOffsetChange = { viewModel.updateYOffset(it) },
        onAlphaChange = { viewModel.updateAlpha(it) },
        onToggleRotation = { viewModel.toggleRotation(it) },
        onToggleOvalMask = { viewModel.toggleOvalMask(it) }
      )

      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // Real MP4 Export Progress & Status Dialog
  ExportProgressDialog(
    exportProgress = uiState.exportProgress,
    onCancel = { viewModel.cancelExport() },
    onDismiss = { viewModel.dismissExportDialog() }
  )
}
