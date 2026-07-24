package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PowerRed
import com.example.ui.theme.TactileButtonBg
import com.example.ui.theme.TactileButtonPress
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    testTag: String = "tactile_button",
    isPowerButton: Boolean = false,
    isAccentButton: Boolean = false,
    buttonSize: Dp = 56.dp,
    shape: Shape = CircleShape,
    accentColor: Color = if (isPowerButton) PowerRed else NeonCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "button_scale"
    )

    val backgroundBrush = if (isPowerButton) {
        Brush.radialGradient(
            colors = listOf(
                if (isPressed) Color(0xFFFF5252) else Color(0xFFD32F2F),
                Color(0xFF8B0000)
            )
        )
    } else if (isAccentButton) {
        Brush.verticalGradient(
            colors = listOf(
                if (isPressed) accentColor.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.2f),
                TactileButtonBg
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (isPressed) TactileButtonPress else Color(0xFF222C42),
                TactileButtonBg
            )
        )
    }

    val borderColor = when {
        isPowerButton -> PowerRed.copy(alpha = 0.8f)
        isPressed || isAccentButton -> accentColor
        else -> DarkCardBorder
    }

    Box(
        modifier = modifier
            .scale(scaleFactor)
            .size(buttonSize)
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 1.5.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label ?: "Button",
                    tint = if (isPowerButton) Color.White else if (isAccentButton) accentColor else TextPrimary,
                    modifier = Modifier.size(if (label != null) 22.dp else 26.dp)
                )
            }
            if (label != null) {
                if (icon != null) Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = if (isPowerButton) Color.White else if (isAccentButton) accentColor else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
