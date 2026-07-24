package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemoteCommand
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

@Composable
fun VolumeChannelRocker(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val rockerBg = Brush.verticalGradient(
        colors = listOf(
            DarkSurface,
            DarkCardBorder
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VOLUME ROCKER COLUMN
        Column(
            modifier = Modifier
                .width(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(rockerBg)
                .border(1.5.dp, DarkCardBorder, RoundedCornerShape(28.dp))
                .padding(vertical = 12.dp)
                .testTag("rocker_volume"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VOL",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.VOLUME_UP) },
                icon = Icons.Default.Add,
                testTag = "btn_vol_up",
                buttonSize = 48.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.MUTE) },
                icon = Icons.Default.VolumeMute,
                testTag = "btn_mute",
                buttonSize = 42.dp,
                isAccentButton = true,
                accentColor = AmberGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.VOLUME_DOWN) },
                icon = Icons.Default.Remove,
                testTag = "btn_vol_down",
                buttonSize = 48.dp
            )
        }

        // CHANNEL ROCKER COLUMN
        Column(
            modifier = Modifier
                .width(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(rockerBg)
                .border(1.5.dp, DarkCardBorder, RoundedCornerShape(28.dp))
                .padding(vertical = 12.dp)
                .testTag("rocker_channel"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CH",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.CHANNEL_UP) },
                icon = Icons.Default.Add,
                testTag = "btn_ch_up",
                buttonSize = 48.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.INPUT) },
                icon = Icons.Default.Input,
                testTag = "btn_input",
                buttonSize = 42.dp,
                isAccentButton = true,
                accentColor = NeonCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            TactileButton(
                onClick = { onCommand(RemoteCommand.CHANNEL_DOWN) },
                icon = Icons.Default.Remove,
                testTag = "btn_ch_down",
                buttonSize = 48.dp
            )
        }
    }
}
