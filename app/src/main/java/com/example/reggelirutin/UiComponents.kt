package com.example.reggelirutin

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TimerCard(
    title: String,
    time: Int,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showReset: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onStartPause: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    strings: Map<String, String>
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 14.sp, color = Color.White, style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatTime(time),
                fontSize = 30.sp,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onStart != null && onPause != null) {
                    RutinButton(onClick = onStart, isActive = isRunning) { Text(strings["start"]!!) }
                    RutinButton(onClick = onPause, isActive = !isRunning && time > 0) { Text(strings["pause"]!!) }
                } else if (onStartPause != null) {
                    RutinButton(onClick = onStartPause, isActive = isRunning) {
                        Text(if (isRunning) strings["pause"]!! else strings["start"]!!)
                    }
                }
                if (showReset && onReset != null) {
                    RutinButton(onClick = onReset) { Text(strings["reset"]!!) }
                }
            }
        }
    }
}

@Composable
fun RutinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val finalContainerColor = containerColor ?: if (isPressed || isActive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = finalContainerColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    currentSet: Int,
    isDone: Boolean,
    onSetIncrement: () -> Unit,
    onSetDecrement: () -> Unit,
    strings: Map<String, String>,
    isCurrent: Boolean = false,
    scale: Float = 1f
) {
    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        isDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> Color.Black.copy(alpha = 0.3f)
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCurrent) 12.dp else 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (isCurrent) Color.Yellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = exercise.name,
                    fontSize = if (isCurrent) 19.sp else 17.sp,
                    fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = if (isCurrent) Color(0xFFFFEB3B) else Color.White
                )
                Text(
                    text = exercise.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = exercise.setsReps,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strings["set"]}: $currentSet/${exercise.totalSets}",
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    RutinButton(onClick = onSetDecrement) { Text("-", fontSize = 18.sp) }
                    Spacer(Modifier.width(8.dp))
                    RutinButton(onClick = onSetIncrement) { Text("+1", fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
fun SunBackground(progress: Float) {
    // Using Material 3 color surface tones if dynamic color is not available
    val surfaceColor = MaterialTheme.colorScheme.surface
    val topColor = lerp(Color.Black, Color(0xFF1A1C1E), 1f - progress) // Dark start
    val skyColor = lerp(topColor, Color(0xFF87CEEB), progress)
    val bottomColor = lerp(Color.Black, surfaceColor, progress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(skyColor, bottomColor)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = this.maxHeight
            val screenWidth = this.maxWidth

            val sunSize = screenWidth * (0.4f + 1.2f * progress)
            val sunY = screenHeight - (screenHeight / 2 * progress)
            val sunAlpha = 0.1f + (0.9f * progress)

            Box(
                modifier = Modifier
                    .offset(y = sunY - (sunSize / 2), x = (screenWidth / 2) - (sunSize / 2))
                    .size(sunSize)
                    .graphicsLayer(alpha = sunAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEB3B), // Bright Yellow
                                Color(0xFFFF9800), // Orange
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}

@Suppress("DEPRECATION")
fun playNotification(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            it.vibrate(700)
        }
    }

    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
    } catch (_: Exception) {}
}
