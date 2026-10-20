package com.oxdm.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oxdm.app.data.PartUiModel

/**
 * Visualizes the segmented multi-part download in real-time.
 * Renders each allocated part's range and its progress.
 */
@Composable
fun SegmentVisualizerBar(
    parts: List<PartUiModel>,
    totalBytes: Long,
    fallbackProgress: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val partTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Full track background
        drawRect(color = trackColor)

        if (parts.isEmpty() || totalBytes <= 0) {
            // Fallback single progress bar if parts aren't yet segmented
            val fillWidth = size.width * fallbackProgress.coerceIn(0f, 1f)
            drawRect(
                color = primaryColor,
                topLeft = Offset.Zero,
                size = Size(fillWidth, size.height)
            )
            return@Canvas
        }

        // Render each active part
        for (part in parts) {
            val partStartRatio = (part.offset.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            val partSizeRatio = (part.size.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            val startX = size.width * partStartRatio
            val partWidth = size.width * partSizeRatio

            // Part boundaries track
            drawRect(
                color = partTrackColor,
                topLeft = Offset(startX, 0f),
                size = Size(partWidth.coerceAtLeast(1f), size.height)
            )

            // Part downloaded bytes
            if (part.size > 0 && part.downloaded > 0) {
                val downloadedRatio = (part.downloaded.toFloat() / part.size.toFloat()).coerceIn(0f, 1f)
                val downloadedWidth = partWidth * downloadedRatio
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(startX, 0f),
                    size = Size(downloadedWidth.coerceAtLeast(1f), size.height)
                )
            }
        }
    }
}
