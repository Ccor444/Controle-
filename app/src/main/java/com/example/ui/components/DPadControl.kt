package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemoteCommand
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TactileButtonBg

@Composable
fun DPadControl(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val dpadOuterBrush = Brush.radialGradient(
        colors = listOf(
            DarkSurfaceVariant,
            TactileButtonBg
        )
    )

    Box(
        modifier = modifier
            .size(230.dp)
            .clip(CircleShape)
            .background(dpadOuterBrush)
            .border(2.dp, DarkCardBorder, CircleShape)
            .testTag("dpad_container"),
        contentAlignment = Alignment.Center
    ) {
        // Up
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.UP) },
                icon = Icons.Default.KeyboardArrowUp,
                testTag = "btn_dpad_up",
                buttonSize = 52.dp
            )
        }

        // Down
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.DOWN) },
                icon = Icons.Default.KeyboardArrowDown,
                testTag = "btn_dpad_down",
                buttonSize = 52.dp
            )
        }

        // Left
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.LEFT) },
                icon = Icons.Default.KeyboardArrowLeft,
                testTag = "btn_dpad_left",
                buttonSize = 52.dp
            )
        }

        // Right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.RIGHT) },
                icon = Icons.Default.KeyboardArrowRight,
                testTag = "btn_dpad_right",
                buttonSize = 52.dp
            )
        }

        // Center OK
        TactileButton(
            onClick = { onCommand(RemoteCommand.OK) },
            label = "OK",
            isAccentButton = true,
            accentColor = NeonCyan,
            testTag = "btn_dpad_ok",
            buttonSize = 68.dp
        )
    }
}
