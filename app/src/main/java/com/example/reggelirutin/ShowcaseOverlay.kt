package com.example.reggelirutin

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShowcaseOverlay(
    targetRect: Rect?,
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean,
    strings: Map<String, String>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {} // Consume clicks
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    blendMode = BlendMode.Clear
                }
                targetRect?.let {
                    // Draw a cleared rounded rect for the target
                    canvas.drawRoundRect(
                        left = it.left - 8.dp.toPx(),
                        top = it.top - 8.dp.toPx(),
                        right = it.right + 8.dp.toPx(),
                        bottom = it.bottom + 8.dp.toPx(),
                        radiusX = 12.dp.toPx(),
                        radiusY = 12.dp.toPx(),
                        paint = paint
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onSkip) {
                    Text(strings["later"] ?: "Later")
                }
                Button(onClick = onNext) {
                    Text(if (isLastStep) (strings["got_it"] ?: "Got it") else (strings["next"] ?: "Next"))
                }
            }
        }
    }
}
