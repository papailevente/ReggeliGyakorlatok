package com.example.reggelirutin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun ShowcaseOverlay(
    targetRect: Rect?,
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    strings: Map<String, String>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) // Crucial for BlendMode.Clear
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {}
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    blendMode = BlendMode.Clear
                }
                targetRect?.let {
                    canvas.drawRoundRect(
                        left = it.left - 12.dp.toPx(),
                        top = it.top - 12.dp.toPx(),
                        right = it.right + 12.dp.toPx(),
                        bottom = it.bottom + 12.dp.toPx(),
                        radiusX = 16.dp.toPx(),
                        radiusY = 16.dp.toPx(),
                        paint = paint
                    )
                }
            }
        }

        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            
            // If target is in top half, show dialog at bottom, and vice versa
            val isTargetInTopHalf = targetRect?.let { ((it.top + it.bottom) / 2f) < screenHeightPx / 2f } ?: true
            
            val alignment = when {
                targetRect == null -> Alignment.Center
                isTargetInTopHalf -> Alignment.BottomCenter
                else -> Alignment.TopCenter
            }

            Column(
                modifier = Modifier
                    .align(alignment)
                    .padding(horizontal = 24.dp, vertical = 80.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(strings["later"] ?: "Later", fontSize = 16.sp)
                    }
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val buttonText = when {
                            isLastStep -> strings["got_it"] ?: "Got it"
                            isFirstStep -> strings["demo"] ?: "Demo"
                            else -> strings["next"] ?: "Next"
                        }
                        Text(text = buttonText, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
