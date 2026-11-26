package com.frenchtoast.wifimapper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.SignalColors

@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  alpha: Float = 0.72f,
  shadowElevation: Dp = 6.dp,
  borderColor: Color = GlassBorderColor,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha),
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = shadowElevation,
    content = content,
  )
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  alpha: Float = 0.72f,
  borderColor: Color = GlassBorderColor,
  content: @Composable ColumnScope.() -> Unit,
) {
  GlassSurface(
    modifier = modifier,
    shape = shape,
    alpha = alpha,
    borderColor = borderColor,
  ) {
    Column(content = content)
  }
}

@Composable
fun SignalQualityLegendPill(
  modifier: Modifier = Modifier,
) {
  GlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    alpha = 0.78f,
    shadowElevation = 4.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      LegendDot(label = "Great", color = SignalColors.StrongGreen)
      LegendDot(label = "Okay", color = SignalColors.FairAmber)
      LegendDot(label = "Weak", color = SignalColors.WeakCrimson)
    }
  }
}

@Composable
private fun LegendDot(label: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
