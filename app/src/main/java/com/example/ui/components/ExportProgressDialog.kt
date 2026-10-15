package com.example.ui.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExportProgress
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSuccess
import kotlin.math.roundToInt

@Composable
fun ExportProgressDialog(
  exportProgress: ExportProgress,
  onCancel: () -> Unit,
  onDismiss: () -> Unit
) {
  if (!exportProgress.isExporting && exportProgress.savedPath == null && exportProgress.errorMessage == null) {
    return
  }

  Dialog(
    onDismissRequest = {
      if (!exportProgress.isExporting) onDismiss()
    },
    properties = DialogProperties(dismissOnBackPress = !exportProgress.isExporting, dismissOnClickOutside = false)
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("export_progress_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon header
        val headerIcon = when {
          exportProgress.errorMessage != null -> Icons.Default.Error
          exportProgress.savedPath != null -> Icons.Default.CheckCircle
          else -> Icons.Default.Movie
        }
        val headerTint = when {
          exportProgress.errorMessage != null -> ErrorRed
          exportProgress.savedPath != null -> StudioSuccess
          else -> StudioPrimary
        }

        Icon(
          headerIcon,
          contentDescription = null,
          tint = headerTint,
          modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = when {
            exportProgress.errorMessage != null -> "Błąd eksportu wideo"
            exportProgress.savedPath != null -> "Eksport zakończony sukcesem!"
            else -> "Eksportowanie filmu MP4..."
          },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (exportProgress.isExporting) {
          LinearProgressIndicator(
            progress = { exportProgress.progress.coerceIn(0f, 1f) },
            color = StudioPrimary,
            trackColor = Color(0xFF1E293B),
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .testTag("export_linear_progress")
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${(exportProgress.progress * 100).roundToInt()}%",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = StudioPrimary
            )
            if (exportProgress.totalSec > 0) {
              Text(
                text = "${exportProgress.currentSec}s / ${exportProgress.totalSec}s",
                fontSize = 12.sp,
                color = Color.LightGray
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = exportProgress.statusText,
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 2
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_cancel_export")
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Anuluj eksport", fontSize = 13.sp)
          }
        } else if (exportProgress.savedPath != null) {
          Text(
            text = "Film został wyeksportowany z oryginalnym dźwiękiem i rozdzielczością do formatu MP4.",
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Text(
            text = exportProgress.savedPath,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 2
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = StudioSuccess),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_close_export_dialog")
          ) {
            Text("Zamknij", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        } else if (exportProgress.errorMessage != null) {
          Text(
            text = exportProgress.errorMessage,
            fontSize = 12.sp,
            color = ErrorRed,
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_close_error_dialog")
          ) {
            Text("Zamknij", color = Color.White)
          }
        }
      }
    }
  }
}
