package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.ml.FaceDetectionHelper
import com.example.ml.FaceTracker
import com.example.model.DetectedFaceInfo
import com.example.model.ExportProgress
import com.example.model.MediaMode
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import com.example.model.VideoInfo
import com.example.util.SampleAssets
import com.example.util.VideoExportEngine
import com.example.util.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class FaceSwapUiState(
  val mediaMode: MediaMode = MediaMode.VIDEO,
  val presetFaces: List<ReplacementFace> = emptyList(),
  val customFaces: List<ReplacementFace> = emptyList(),
  val swapConfig: SwapConfig = SwapConfig(),

  // Photo
  val originalPhoto: Bitmap? = null,
  val detectedPhotoFaces: List<DetectedFaceInfo> = emptyList(),
  val renderedPhoto: Bitmap? = null,
  val isDetectingPhoto: Boolean = false,
  val selectedFaceIdToMap: Int = 0,

  // Video
  val videoUri: Uri? = null,
  val videoInfo: VideoInfo? = null,
  val currentVideoTimeMs: Long = 0L,
  val isPlayingVideo: Boolean = false,
  val currentVideoFrame: Bitmap? = null,
  val detectedVideoFaces: List<DetectedFaceInfo> = emptyList(),
  val renderedVideoFrame: Bitmap? = null,
  val isExtractingVideoFrame: Boolean = false,
  val showOriginalComparison: Boolean = false,

  // Full Video Export Progress
  val exportProgress: ExportProgress = ExportProgress(),
  val toastMessage: String? = null
) {
  val allAvailableFaces: List<ReplacementFace>
    get() = customFaces + presetFaces
}

class FaceSwapViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(FaceSwapUiState())
  val uiState: StateFlow<FaceSwapUiState> = _uiState.asStateFlow()

  val exoPlayer: ExoPlayer by lazy {
    ExoPlayer.Builder(application).build().apply {
      repeatMode = Player.REPEAT_MODE_ALL
      addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          _uiState.update { it.copy(isPlayingVideo = isPlaying) }
          if (isPlaying) {
            startPositionTracking()
          } else {
            stopPositionTracking()
            // When paused, extract high-res frame at exact pause timestamp
            refreshCurrentVideoFrame(currentPosition)
          }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_READY) {
            _uiState.update { it.copy(currentVideoTimeMs = currentPosition) }
          }
        }
      })
    }
  }

  private var positionTrackJob: Job? = null
  private val videoFaceTracker = FaceTracker()
  private var exportJob: Job? = null
  private val isExportCancelled = AtomicBoolean(false)

  init {
    val presets = SampleAssets.getPresetFaces(application)
    val defaultGlobal = presets.firstOrNull()
    _uiState.update {
      it.copy(
        presetFaces = presets,
        swapConfig = it.swapConfig.copy(globalReplacement = defaultGlobal)
      )
    }
  }

  fun setMediaMode(mode: MediaMode) {
    if (_uiState.value.mediaMode == mode) return
    if (_uiState.value.mediaMode == MediaMode.VIDEO) {
      exoPlayer.pause()
    }
    _uiState.update { it.copy(mediaMode = mode) }
    if (mode == MediaMode.PHOTO && _uiState.value.originalPhoto == null) {
      loadSamplePhoto()
    }
  }

  fun toggleComparison(showOriginal: Boolean) {
    _uiState.update { it.copy(showOriginalComparison = showOriginal) }
  }

  fun loadSamplePhoto() {
    viewModelScope.launch {
      _uiState.update { it.copy(isDetectingPhoto = true) }
      val sampleBmp = SampleAssets.generateSampleGroupPhoto()
      processLoadedPhoto(sampleBmp)
    }
  }

  fun loadPhotoFromUri(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isDetectingPhoto = true) }
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap != null) {
          processLoadedPhoto(bitmap)
        } else {
          _uiState.update {
            it.copy(
              isDetectingPhoto = false,
              toastMessage = "Nieprawidłowy plik obrazu"
            )
          }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isDetectingPhoto = false,
            toastMessage = "Błąd odczytu pliku: ${e.localizedMessage}"
          )
        }
      }
    }
  }

  private suspend fun processLoadedPhoto(bitmap: Bitmap) {
    val faces = FaceDetectionHelper.detectFaces(bitmap)
    _uiState.update {
      it.copy(
        originalPhoto = bitmap,
        detectedPhotoFaces = faces,
        isDetectingPhoto = false,
        selectedFaceIdToMap = faces.firstOrNull()?.id ?: 0,
        toastMessage = if (faces.isEmpty()) "Nie wykryto żadnej twarzy na zdjęciu" else "Wykryto ${faces.size} twarzy"
      )
    }
    reRenderPhoto()
  }

  fun reRenderPhoto() {
    val state = _uiState.value
    val orig = state.originalPhoto ?: return
    val faces = state.detectedPhotoFaces
    viewModelScope.launch(Dispatchers.Default) {
      val rendered = FaceDetectionHelper.renderFaceSwap(orig, faces, state.swapConfig)
      _uiState.update { it.copy(renderedPhoto = rendered) }
    }
  }

  fun loadVideoFromUri(context: Context, uri: Uri) {
    exoPlayer.pause()
    viewModelScope.launch {
      _uiState.update { it.copy(isExtractingVideoFrame = true) }
      val info = VideoProcessor.extractVideoInfo(context, uri)
      if (info != null && info.durationMs > 0L) {
        videoFaceTracker.reset()
        _uiState.update {
          it.copy(
            videoUri = uri,
            videoInfo = info,
            currentVideoTimeMs = 0L,
            toastMessage = "Wczytano wideo: ${info.durationMs / 1000}s (${if (info.isVertical) "Pionowe" else "Poziome"})"
          )
        }

        // Configure ExoPlayer with real media and real audio
        withContext(Dispatchers.Main) {
          exoPlayer.setMediaItem(MediaItem.fromUri(uri))
          exoPlayer.prepare()
          exoPlayer.seekTo(0L)
        }

        refreshCurrentVideoFrame(0L)
      } else {
        _uiState.update {
          it.copy(
            isExtractingVideoFrame = false,
            toastMessage = "Błąd: Plik wideo jest nieobsługiwany lub uszkodzony"
          )
        }
      }
    }
  }

  fun toggleVideoPlayback() {
    if (exoPlayer.isPlaying) {
      exoPlayer.pause()
    } else {
      exoPlayer.play()
    }
  }

  fun seekVideo(timeMs: Long) {
    exoPlayer.seekTo(timeMs)
    _uiState.update { it.copy(currentVideoTimeMs = timeMs) }
    refreshCurrentVideoFrame(timeMs)
  }

  private fun startPositionTracking() {
    positionTrackJob?.cancel()
    positionTrackJob = viewModelScope.launch {
      while (isActive && exoPlayer.isPlaying) {
        val pos = exoPlayer.currentPosition
        _uiState.update { it.copy(currentVideoTimeMs = pos) }
        delay(250) // Update UI scrubber
      }
    }
  }

  private fun stopPositionTracking() {
    positionTrackJob?.cancel()
    positionTrackJob = null
  }

  private fun refreshCurrentVideoFrame(timeMs: Long) {
    val uri = _uiState.value.videoUri ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isExtractingVideoFrame = true) }
      val frameBmp = VideoProcessor.extractFrameAtTime(getApplication(), uri, timeMs * 1000L)
      if (frameBmp != null) {
        val faces = FaceDetectionHelper.detectFaces(frameBmp)
        val smoothed = videoFaceTracker.update(faces)
        val rendered = FaceDetectionHelper.renderFaceSwapSmoothed(frameBmp, smoothed, _uiState.value.swapConfig)
        _uiState.update {
          it.copy(
            currentVideoFrame = frameBmp,
            detectedVideoFaces = faces,
            renderedVideoFrame = rendered,
            isExtractingVideoFrame = false
          )
        }
      } else {
        _uiState.update { it.copy(isExtractingVideoFrame = false) }
      }
    }
  }

  fun reRenderVideoCurrentFrame() {
    val raw = _uiState.value.currentVideoFrame ?: return
    val faces = _uiState.value.detectedVideoFaces
    val config = _uiState.value.swapConfig
    viewModelScope.launch(Dispatchers.Default) {
      val smoothed = videoFaceTracker.update(faces)
      val rendered = FaceDetectionHelper.renderFaceSwapSmoothed(raw, smoothed, config)
      _uiState.update { it.copy(renderedVideoFrame = rendered) }
    }
  }

  fun setSwapMode(singleForAll: Boolean) {
    _uiState.update {
      it.copy(swapConfig = it.swapConfig.copy(isSingleForAll = singleForAll))
    }
    reRenderActiveMedia()
  }

  fun setGlobalReplacementFace(face: ReplacementFace) {
    _uiState.update {
      it.copy(swapConfig = it.swapConfig.copy(globalReplacement = face))
    }
    reRenderActiveMedia()
  }

  fun setIndividualFaceMapping(faceId: Int, face: ReplacementFace) {
    _uiState.update {
      val updatedMap = it.swapConfig.individualMappings.toMutableMap()
      updatedMap[faceId] = face
      it.copy(swapConfig = it.swapConfig.copy(individualMappings = updatedMap))
    }
    reRenderActiveMedia()
  }

  fun selectFaceForMapping(faceId: Int) {
    _uiState.update { it.copy(selectedFaceIdToMap = faceId) }
  }

  fun updateScale(scale: Float) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(scale = scale)) }
    reRenderActiveMedia()
  }

  fun updateYOffset(offset: Float) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(yOffset = offset)) }
    reRenderActiveMedia()
  }

  fun updateAlpha(alpha: Float) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(alpha = alpha)) }
    reRenderActiveMedia()
  }

  fun toggleRotation(enabled: Boolean) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(rotationEnabled = enabled)) }
    reRenderActiveMedia()
  }

  fun toggleOvalMask(enabled: Boolean) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(isOvalMask = enabled)) }
    reRenderActiveMedia()
  }

  fun addCustomFaceFromUri(context: Context, uri: Uri, customName: String = "Własna Twarz") {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val stream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(stream)
        stream?.close()
        if (bitmap != null) {
          val replacement = FaceDetectionHelper.extractFaceForReplacement(bitmap, customName)
          _uiState.update {
            val list = listOf(replacement) + it.customFaces
            val config = if (it.swapConfig.globalReplacement == null) {
              it.swapConfig.copy(globalReplacement = replacement)
            } else it.swapConfig
            it.copy(
              customFaces = list,
              swapConfig = config,
              toastMessage = "Dodano twarz: $customName"
            )
          }
          reRenderActiveMedia()
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(toastMessage = "Błąd dodawania twarzy: ${e.localizedMessage}") }
      }
    }
  }

  private fun reRenderActiveMedia() {
    if (_uiState.value.mediaMode == MediaMode.PHOTO) {
      reRenderPhoto()
    } else {
      reRenderVideoCurrentFrame()
    }
  }

  /**
   * Real full video export to MP4 with original audio track, duration, and orientation preserved.
   */
  fun exportFullVideo(context: Context) {
    val uri = _uiState.value.videoUri
    if (uri == null) {
      _uiState.update { it.copy(toastMessage = "Wybierz najpierw plik wideo do eksportu") }
      return
    }

    exoPlayer.pause()
    isExportCancelled.set(false)

    exportJob?.cancel()
    exportJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          exportProgress = ExportProgress(
            isExporting = true,
            statusText = "Inicjalizacja kodera MP4..."
          )
        )
      }

      val result = VideoExportEngine.exportVideo(
        context = context,
        sourceUri = uri,
        swapConfig = _uiState.value.swapConfig,
        isCancelled = isExportCancelled,
        onProgress = { progress, currentFrame, totalFrames, currentSec, totalSec, status ->
          _uiState.update {
            it.copy(
              exportProgress = it.exportProgress.copy(
                isExporting = true,
                progress = progress,
                currentFrame = currentFrame,
                totalFrames = totalFrames,
                currentSec = currentSec,
                totalSec = totalSec,
                statusText = status
              )
            )
          }
        }
      )

      result.onSuccess { savedPath ->
        _uiState.update {
          it.copy(
            exportProgress = it.exportProgress.copy(
              isExporting = false,
              progress = 1.0f,
              statusText = "Gotowe! Zapisano wideo MP4.",
              savedPath = savedPath
            ),
            toastMessage = "Wyeksportowano pomyślnie do MP4!"
          )
        }
      }.onFailure { error ->
        _uiState.update {
          it.copy(
            exportProgress = it.exportProgress.copy(
              isExporting = false,
              errorMessage = error.localizedMessage ?: "Błąd eksportu wideo",
              statusText = "Niepowodzenie eksportu"
            ),
            toastMessage = "Błąd eksportu: ${error.localizedMessage}"
          )
        }
      }
    }
  }

  fun cancelExport() {
    isExportCancelled.set(true)
    exportJob?.cancel()
    _uiState.update {
      it.copy(
        exportProgress = ExportProgress(isExporting = false, statusText = "Eksport anulowany"),
        toastMessage = "Eksport wideo został anulowany"
      )
    }
  }

  fun dismissExportDialog() {
    _uiState.update { it.copy(exportProgress = ExportProgress()) }
  }

  fun exportCurrentPhoto(context: Context) {
    viewModelScope.launch {
      val bmp = _uiState.value.renderedPhoto
      if (bmp == null) {
        _uiState.update { it.copy(toastMessage = "Brak wygenerowanego zdjęcia") }
        return@launch
      }
      val path = VideoProcessor.saveBitmapToFile(context, bmp, "faceswap_photo")
      _uiState.update {
        it.copy(toastMessage = "Zapisano zdjęcie w pamięci telefonu: $path")
      }
    }
  }

  fun dismissToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    stopPositionTracking()
    exoPlayer.release()
  }
}
