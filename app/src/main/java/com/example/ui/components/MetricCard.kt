package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MetricCard styled 1:1 with the reference UI (Picture, Video, My File, Apps).
 * Features a soft-shadowed pillowy card, vibrant pastel squircle icon badge with gradient,
 * high-contrast title, value, and status subtitle.
 */
@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  badgeGradient: Brush,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .shadow(
        elevation = 2.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color(0x10000000),
        spotColor = Color(0x187E42F5)
      )
      .clip(RoundedCornerShape(24.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
      .padding(horizontal = 14.dp, vertical = 14.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Pastel Glowing Squircle Badge with rich gradient (matching reference screenshot)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(46.dp)
          .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(16.dp),
            ambientColor = Color(0x14000000),
            spotColor = accentColor.copy(alpha = 0.5f)
          )
          .clip(RoundedCornerShape(16.dp))
          .background(badgeGradient)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 15.sp,
          maxLines = 1
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = accentColor,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp,
          maxLines = 1
        )
      }
    }
  }
}
