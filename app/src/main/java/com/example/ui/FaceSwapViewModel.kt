package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ml.FaceDetectionHelper
import com.example.model.DetectedFaceInfo
import com.example.model.MediaMode
import com.example.model.ProcessingProgress
import com.example.model.ReplacementFace
import com.example.model.SwapConfig
import com.example.model.VideoInfo
import com.example.util.SampleAssets
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

data class FaceSwapUiState(
  val mediaMode: MediaMode = MediaMode.PHOTO,
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
  val isDemoVideo: Boolean = false,
  val currentVideoTimeMs: Long = 0L,
  val isPlayingVideo: Boolean = false,
  val currentVideoFrame: Bitmap? = null,
  val detectedVideoFaces: List<DetectedFaceInfo> = emptyList(),
  val renderedVideoFrame: Bitmap? = null,
  val isExtractingVideoFrame: Boolean = false,

  // Export / Progress
  val processingProgress: ProcessingProgress = ProcessingProgress(),
  val toastMessage: String? = null
) {
  val allAvailableFaces: List<ReplacementFace>
    get() = customFaces + presetFaces
}

class FaceSwapViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(FaceSwapUiState())
  val uiState: StateFlow<FaceSwapUiState> = _uiState.asStateFlow()

  private var videoPlayJob: Job? = null
  private var demoFrameIndex = 0

  init {
    val presets = SampleAssets.getPresetFaces(application)
    val defaultGlobal = presets.firstOrNull()
    _uiState.update {
      it.copy(
        presetFaces = presets,
        swapConfig = it.swapConfig.copy(globalReplacement = defaultGlobal)
      )
    }
    // Load sample group photo on first launch so user sees instant capability
    loadSamplePhoto()
  }

  fun setMediaMode(mode: MediaMode) {
    if (_uiState.value.mediaMode == mode) return
    pauseVideo()
    _uiState.update { it.copy(mediaMode = mode) }
    if (mode == MediaMode.VIDEO && _uiState.value.videoInfo == null && !_uiState.value.isDemoVideo) {
      loadDemoVideo()
    }
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
              toastMessage = "Nie udało się wczytać zdjęcia"
            )
          }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isDetectingPhoto = false,
            toastMessage = "Błąd otwierania pliku: ${e.localizedMessage}"
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
        selectedFaceIdToMap = faces.firstOrNull()?.id ?: 0
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

  fun loadDemoVideo() {
    pauseVideo()
    demoFrameIndex = 0
    val totalFrames = 120
    val durationMs = (totalFrames / 24) * 1000L
    _uiState.update {
      it.copy(
        videoUri = null,
        isDemoVideo = true,
        videoInfo = VideoInfo(
          uriString = "demo://sample_video",
          durationMs = durationMs,
          width = 960,
          height = 540
        ),
        currentVideoTimeMs = 0L,
        toastMessage = "Wczytano wideo demonstracyjne"
      )
    }
    renderDemoVideoFrame(0)
  }

  fun loadVideoFromUri(context: Context, uri: Uri) {
    pauseVideo()
    viewModelScope.launch {
      _uiState.update { it.copy(isExtractingVideoFrame = true) }
      val info = VideoProcessor.extractVideoInfo(context, uri)
      if (info != null) {
        _uiState.update {
          it.copy(
            videoUri = uri,
            videoInfo = info,
            isDemoVideo = false,
            currentVideoTimeMs = 0L,
            toastMessage = "Wczytano wideo: ${info.durationMs / 1000}s (${info.width}x${info.height})"
          )
        }
        extractAndRenderVideoFrame(context, 0L)
      } else {
        _uiState.update {
          it.copy(
            isExtractingVideoFrame = false,
            toastMessage = "Nie udało się odczytać pliku wideo"
          )
        }
      }
    }
  }

  fun toggleVideoPlayback(context: Context) {
    if (_uiState.value.isPlayingVideo) {
      pauseVideo()
    } else {
      playVideo(context)
    }
  }

  private fun playVideo(context: Context) {
    _uiState.update { it.copy(isPlayingVideo = true) }
    videoPlayJob?.cancel()
    videoPlayJob = viewModelScope.launch {
      val state = _uiState.value
      val duration = state.videoInfo?.durationMs ?: 5000L

      while (isActive && _uiState.value.isPlayingVideo) {
        if (_uiState.value.isDemoVideo) {
          demoFrameIndex = (demoFrameIndex + 1) % 120
          val timeMs = (demoFrameIndex.toFloat() / 120f * duration).toLong()
          _uiState.update { it.copy(currentVideoTimeMs = timeMs) }
          renderDemoVideoFrame(demoFrameIndex)
          delay(42) // ~24 FPS
        } else {
          val uri = _uiState.value.videoUri ?: break
          var nextTime = _uiState.value.currentVideoTimeMs + 100
          if (nextTime >= duration) nextTime = 0
          _uiState.update { it.copy(currentVideoTimeMs = nextTime) }
          extractAndRenderVideoFrame(context, nextTime)
          delay(100)
        }
      }
    }
  }

  fun pauseVideo() {
    videoPlayJob?.cancel()
    videoPlayJob = null
    _uiState.update { it.copy(isPlayingVideo = false) }
  }

  fun seekVideo(context: Context, timeMs: Long) {
    _uiState.update { it.copy(currentVideoTimeMs = timeMs) }
    if (_uiState.value.isDemoVideo) {
      val duration = _uiState.value.videoInfo?.durationMs ?: 5000L
      val fraction = (timeMs.toFloat() / duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
      demoFrameIndex = (fraction * 119).toInt()
      renderDemoVideoFrame(demoFrameIndex)
    } else {
      viewModelScope.launch {
        extractAndRenderVideoFrame(context, timeMs)
      }
    }
  }

  private fun renderDemoVideoFrame(frameIndex: Int) {
    viewModelScope.launch(Dispatchers.Default) {
      val raw = VideoProcessor.generateDemoVideoFrame(frameIndex)
      val faces = FaceDetectionHelper.detectFaces(raw)
      val rendered = FaceDetectionHelper.renderFaceSwap(raw, faces, _uiState.value.swapConfig)
      _uiState.update {
        it.copy(
          currentVideoFrame = raw,
          detectedVideoFaces = faces,
          renderedVideoFrame = rendered,
          isExtractingVideoFrame = false
        )
      }
    }
  }

  private suspend fun extractAndRenderVideoFrame(context: Context, timeMs: Long) {
    val uri = _uiState.value.videoUri ?: return
    _uiState.update { it.copy(isExtractingVideoFrame = true) }
    val frameBmp = VideoProcessor.extractFrameAtTime(context, uri, timeMs * 1000L)
    if (frameBmp != null) {
      val faces = FaceDetectionHelper.detectFaces(frameBmp)
      val rendered = FaceDetectionHelper.renderFaceSwap(frameBmp, faces, _uiState.value.swapConfig)
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

  fun reRenderVideoCurrentFrame() {
    val raw = _uiState.value.currentVideoFrame ?: return
    val faces = _uiState.value.detectedVideoFaces
    val config = _uiState.value.swapConfig
    viewModelScope.launch(Dispatchers.Default) {
      val rendered = FaceDetectionHelper.renderFaceSwap(raw, faces, config)
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

  fun updateXOffset(offset: Float) {
    _uiState.update { it.copy(swapConfig = it.swapConfig.copy(xOffset = offset)) }
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

  fun exportCurrentMedia(context: Context) {
    viewModelScope.launch {
      val bmp = if (_uiState.value.mediaMode == MediaMode.PHOTO) {
        _uiState.value.renderedPhoto
      } else {
        _uiState.value.renderedVideoFrame
      }

      if (bmp == null) {
        _uiState.update { it.copy(toastMessage = "Brak wygenerowanego obrazu do zapisu") }
        return@launch
      }

      val path = VideoProcessor.saveBitmapToFile(context, bmp, "faceswap_export")
      _uiState.update {
        it.copy(toastMessage = "Zapisano pomyślnie w pamięci telefonu: $path")
      }
    }
  }

  fun dismissToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    pauseVideo()
  }
}
