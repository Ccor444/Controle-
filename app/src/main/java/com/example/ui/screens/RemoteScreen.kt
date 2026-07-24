package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemoteCommand
import com.example.ui.components.DPadControl
import com.example.ui.components.TactileButton
import com.example.ui.components.VolumeChannelRocker
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PowerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RemoteScreen(
    onCommand: (RemoteCommand) -> Unit,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showNumberPad by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP CONTROLS ROW (Power, Home, Menu, Back)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.POWER) },
                icon = Icons.Default.PowerSettingsNew,
                isPowerButton = true,
                testTag = "btn_power",
                buttonSize = 58.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.HOME) },
                icon = Icons.Default.Home,
                label = "Home",
                testTag = "btn_home",
                buttonSize = 50.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.MENU) },
                icon = Icons.Default.Menu,
                label = "Menu",
                testTag = "btn_menu",
                buttonSize = 50.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.BACK) },
                icon = Icons.Default.ArrowBack,
                label = "Voltar",
                testTag = "btn_back",
                buttonSize = 50.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DIRECTIONAL PAD (D-PAD)
        DPadControl(
            onCommand = onCommand,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // VOLUME & CHANNEL ROCKERS
        VolumeChannelRocker(
            onCommand = onCommand,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // MEDIA PLAYBACK BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TactileButton(
                onClick = { onCommand(RemoteCommand.REWIND) },
                icon = Icons.Default.FastRewind,
                testTag = "btn_rewind",
                buttonSize = 44.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.PLAY) },
                icon = Icons.Default.PlayArrow,
                isAccentButton = true,
                accentColor = NeonCyan,
                testTag = "btn_play",
                buttonSize = 48.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.PAUSE) },
                icon = Icons.Default.Pause,
                isAccentButton = true,
                accentColor = AmberGold,
                testTag = "btn_pause",
                buttonSize = 48.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.STOP) },
                icon = Icons.Default.Stop,
                testTag = "btn_stop",
                buttonSize = 44.dp
            )

            TactileButton(
                onClick = { onCommand(RemoteCommand.FORWARD) },
                icon = Icons.Default.FastForward,
                testTag = "btn_forward",
                buttonSize = 44.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK APP LAUNCHERS ROW
        Text(
            text = "ATALHOS RÁPIDOS DE APPS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppLauncherButton("Netflix", PowerRed) { onLaunchApp("Netflix") }
            AppLauncherButton("YouTube", PowerRed) { onLaunchApp("YouTube") }
            AppLauncherButton("Prime", NeonCyan) { onLaunchApp("Prime Video") }
            AppLauncherButton("Disney+", NeonCyan) { onLaunchApp("Disney+") }
            AppLauncherButton("Spotify", AmberGold) { onLaunchApp("Spotify") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NUMBER PAD EXPANDABLE TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .clickable { showNumberPad = !showNumberPad }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Numbers,
                    contentDescription = "Teclado Numérico",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Teclado Numérico (0-9)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (showNumberPad) "Ocultar ▲" else "Exibir ▼",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(visible = showNumberPad) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                val numGrid = listOf(
                    listOf(RemoteCommand.NUM_1, RemoteCommand.NUM_2, RemoteCommand.NUM_3),
                    listOf(RemoteCommand.NUM_4, RemoteCommand.NUM_5, RemoteCommand.NUM_6),
                    listOf(RemoteCommand.NUM_7, RemoteCommand.NUM_8, RemoteCommand.NUM_9)
                )

                for (row in numGrid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (cmd in row) {
                            TactileButton(
                                onClick = { onCommand(cmd) },
                                label = cmd.label,
                                testTag = "btn_num_${cmd.label}",
                                buttonSize = 48.dp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TactileButton(
                        onClick = { onCommand(RemoteCommand.NUM_0) },
                        label = "0",
                        testTag = "btn_num_0",
                        buttonSize = 48.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppLauncherButton(
    name: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(62.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("btn_app_$name"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
