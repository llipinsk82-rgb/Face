package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SwapConfig
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.StudioPrimary
import kotlin.math.roundToInt

@Composable
fun TuningControlsCard(
  config: SwapConfig,
  onScaleChange: (Float) -> Unit,
  onYOffsetChange: (Float) -> Unit,
  onAlphaChange: (Float) -> Unit,
  onToggleRotation: (Boolean) -> Unit,
  onToggleOvalMask: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("tuning_controls_card"),
    shape = RoundedCornerShape(16.dp),
    color = DarkSurfaceCard,
    border = BorderStroke(1.dp, DarkBorder)
  ) {
    Column(
      modifier = Modifier.padding(14.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Default.Tune,
            contentDescription = null,
            tint = StudioPrimary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "Zaawansowane dopasowanie",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
          )
        }

        Icon(
          if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
          tint = Color.LightGray
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          // Honest technology disclosure
          Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
              Text(
                text = "Silnik dopasowuje maskę na podstawie punktów oczu i owalu głowy z wygładzaniem ruchu. Nie symuluje ruchów ust (lip-sync).",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
              )
            }
          }

          // Scale Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Powiększenie:", fontSize = 12.sp, color = Color.LightGray)
            Text("${(config.scale * 100).roundToInt()}%", fontSize = 12.sp, color = StudioPrimary, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = config.scale,
            onValueChange = onScaleChange,
            valueRange = 0.8f..1.5f,
            colors = SliderDefaults.colors(
              thumbColor = StudioPrimary,
              activeTrackColor = StudioPrimary
            ),
            modifier = Modifier.testTag("slider_scale")
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Y-Offset Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Pozycja pionowa (Góra/Dół):", fontSize = 12.sp, color = Color.LightGray)
            Text("${(config.yOffset * 100).roundToInt()}%", fontSize = 12.sp, color = Color.White)
          }
          Slider(
            value = config.yOffset,
            onValueChange = onYOffsetChange,
            valueRange = -0.25f..0.25f,
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = Color.White
            ),
            modifier = Modifier.testTag("slider_y_offset")
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Alpha Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Krycie (Przezroczystość):", fontSize = 12.sp, color = Color.LightGray)
            Text("${(config.alpha * 100).roundToInt()}%", fontSize = 12.sp, color = Color.White)
          }
          Slider(
            value = config.alpha,
            onValueChange = onAlphaChange,
            valueRange = 0.3f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = Color.White,
              activeTrackColor = Color.White
            ),
            modifier = Modifier.testTag("slider_alpha")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Rotation match toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Śledzenie pochylenia głowy", fontSize = 13.sp, color = Color.White)
              Text("Dopasowuje kąt maski do linii oczu", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
              checked = config.rotationEnabled,
              onCheckedChange = onToggleRotation,
              colors = SwitchDefaults.colors(checkedThumbColor = StudioPrimary),
              modifier = Modifier.testTag("switch_rotation")
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Oval mask toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Wygładzony owal twarzy", fontSize = 13.sp, color = Color.White)
              Text("Eliminuje ostre krawędzie maski", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
              checked = config.isOvalMask,
              onCheckedChange = onToggleOvalMask,
              colors = SwitchDefaults.colors(checkedThumbColor = StudioPrimary),
              modifier = Modifier.testTag("switch_oval_mask")
            )
          }
        }
      }
    }
  }
}
