package com.example.ml

import com.example.model.DetectedFaceInfo
import kotlin.math.abs
import kotlin.math.hypot

data class SmoothedFaceState(
  val trackId: Int,
  var centerX: Float,
  var centerY: Float,
  var width: Float,
  var height: Float,
  var angleDeg: Float,
  var missingFrames: Int = 0,
  var rawFaceInfo: DetectedFaceInfo
)

class FaceTracker(
  private val smoothingFactor: Float = 0.40f,
  private val maxMissingFrames: Int = 4
) {
  private val tracks = mutableListOf<SmoothedFaceState>()
  private var nextTrackId = 1

  @Synchronized
  fun update(detectedFaces: List<DetectedFaceInfo>): List<SmoothedFaceState> {
    if (detectedFaces.isEmpty()) {
      // Increment missing frame counter for all active tracks
      tracks.forEach { it.missingFrames++ }
      tracks.removeAll { it.missingFrames > maxMissingFrames }
      return emptyList()
    }

    val unmatchedDetections = detectedFaces.toMutableList()
    val updatedTracks = mutableListOf<SmoothedFaceState>()

    // Pass 1: Match by ML Kit trackingId if present
    val trackIdMatches = mutableSetOf<Int>()
    for (det in detectedFaces) {
      if (det.trackingId != null) {
        val existing = tracks.find { it.rawFaceInfo.trackingId == det.trackingId }
        if (existing != null) {
          applySmoothing(existing, det)
          existing.missingFrames = 0
          existing.rawFaceInfo = det
          updatedTracks.add(existing)
          unmatchedDetections.remove(det)
          trackIdMatches.add(existing.trackId)
        }
      }
    }

    // Pass 2: Match remaining detections to remaining tracks by minimum Euclidean distance
    val remainingTracks = tracks.filter { it.trackId !in trackIdMatches }
    for (existing in remainingTracks) {
      if (unmatchedDetections.isEmpty()) break

      // Find closest detection within reasonable distance threshold (max 1.5x face width)
      val maxAllowedDistance = existing.width * 1.5f
      var bestDet: DetectedFaceInfo? = null
      var bestDist = Float.MAX_VALUE

      for (det in unmatchedDetections) {
        val dist = hypot(det.anchorCenterX - existing.centerX, det.anchorCenterY - existing.centerY)
        if (dist < bestDist && dist <= maxAllowedDistance) {
          bestDist = dist
          bestDet = det
        }
      }

      if (bestDet != null) {
        applySmoothing(existing, bestDet)
        existing.missingFrames = 0
        existing.rawFaceInfo = bestDet
        updatedTracks.add(existing)
        unmatchedDetections.remove(bestDet)
      } else {
        existing.missingFrames++
      }
    }

    // Pass 3: Create new tracks for remaining unmatched detections
    for (newDet in unmatchedDetections) {
      val newTrack = SmoothedFaceState(
        trackId = nextTrackId++,
        centerX = newDet.anchorCenterX,
        centerY = newDet.anchorCenterY,
        width = newDet.bounds.width().toFloat(),
        height = newDet.bounds.height().toFloat(),
        angleDeg = newDet.rollAngle,
        missingFrames = 0,
        rawFaceInfo = newDet
      )
      tracks.add(newTrack)
      updatedTracks.add(newTrack)
    }

    // Remove stale tracks
    tracks.removeAll { it.missingFrames > maxMissingFrames }

    return updatedTracks
  }

  private fun applySmoothing(track: SmoothedFaceState, det: DetectedFaceInfo) {
    val alpha = smoothingFactor.coerceIn(0.1f, 1.0f)

    // Smooth position and dimensions using Exponential Moving Average
    track.centerX = track.centerX * (1f - alpha) + det.anchorCenterX * alpha
    track.centerY = track.centerY * (1f - alpha) + det.anchorCenterY * alpha
    track.width = track.width * (1f - alpha) + det.bounds.width().toFloat() * alpha
    track.height = track.height * (1f - alpha) + det.bounds.height().toFloat() * alpha

    // Smooth angle handling circular wrapping around 180/-180 deg
    var angleDiff = det.rollAngle - track.angleDeg
    while (angleDiff > 180f) angleDiff -= 360f
    while (angleDiff < -180f) angleDiff += 360f

    // Avoid smoothing sudden 90+ deg jumps which indicate profile flips
    if (abs(angleDiff) < 80f) {
      track.angleDeg += angleDiff * alpha
    } else {
      track.angleDeg = det.rollAngle
    }
  }

  @Synchronized
  fun reset() {
    tracks.clear()
    nextTrackId = 1
  }
}
