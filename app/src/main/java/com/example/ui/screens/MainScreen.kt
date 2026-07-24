package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ConnectionState
import com.example.ui.components.ConnectionBar
import com.example.ui.components.TouchpadView
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RemoteTab
import com.example.ui.viewmodel.RemoteViewModel

@Composable
fun MainScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val connectionState by viewModel.remoteCore.connectionState.collectAsState()
    val lastLogMessage by viewModel.remoteCore.lastCommandLog.collectAsState()
    val sleepTimerMinutes by viewModel.remoteCore.sleepTimerMinutes.collectAsState()

    val discoveredTvs by viewModel.networkScanner.discoveredTvs.collectAsState(initial = emptyList())
    val isScanning by viewModel.networkScanner.isScanning.collectAsState(initial = false)
    val savedTvs by viewModel.savedTvs.collectAsState()

    val manualIp by viewModel.manualIpInput.collectAsState()
    val selectedBrand by viewModel.selectedBrandInput.collectAsState()

    val chatLog by viewModel.aiChatLog.collectAsState()
    val isProcessingAi by viewModel.isProcessingAi.collectAsState()
    val isListening by viewModel.voiceHelper.isListening.collectAsState()
    val spokenText by viewModel.voiceHelper.spokenText.collectAsState()

    val activeDevice = when (val state = connectionState) {
        is ConnectionState.Connected -> state.device
        is ConnectionState.Connecting -> state.device
        else -> null
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                ConnectionBar(
                    connectionState = connectionState,
                    sleepTimerMinutes = sleepTimerMinutes,
                    lastLogMessage = lastLogMessage,
                    onDevicesClick = { viewModel.selectTab(RemoteTab.DEVICES) },
                    onCancelTimerClick = { viewModel.remoteCore.cancelSleepTimer() }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == RemoteTab.REMOTE,
                    onClick = { viewModel.selectTab(RemoteTab.REMOTE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Controle"
                        )
                    },
                    label = { Text("Controle", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = DarkCardBorder,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_remote")
                )

                NavigationBarItem(
                    selected = selectedTab == RemoteTab.TOUCHPAD,
                    onClick = { viewModel.selectTab(RemoteTab.TOUCHPAD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = "Touchpad"
                        )
                    },
                    label = { Text("Touchpad", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = DarkCardBorder,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_touchpad")
                )

                NavigationBarItem(
                    selected = selectedTab == RemoteTab.KEYBOARD,
                    onClick = { viewModel.selectTab(RemoteTab.KEYBOARD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Teclado"
                        )
                    },
                    label = { Text("Teclado", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = DarkCardBorder,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_keyboard")
                )

                NavigationBarItem(
                    selected = selectedTab == RemoteTab.AI_VOICE,
                    onClick = { viewModel.selectTab(RemoteTab.AI_VOICE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Voz IA"
                        )
                    },
                    label = { Text("Voz IA", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberGold,
                        selectedTextColor = AmberGold,
                        indicatorColor = DarkCardBorder,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_ai")
                )

                NavigationBarItem(
                    selected = selectedTab == RemoteTab.DEVICES,
                    onClick = { viewModel.selectTab(RemoteTab.DEVICES) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TVs"
                        )
                    },
                    label = { Text("TVs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = DarkCardBorder,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_devices")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                RemoteTab.REMOTE -> RemoteScreen(
                    onCommand = { viewModel.sendCommand(it) },
                    onLaunchApp = { viewModel.launchApp(it) }
                )
                RemoteTab.TOUCHPAD -> TouchpadView(
                    onCommand = { viewModel.sendCommand(it) },
                    onMouseMove = { dx, dy -> viewModel.sendMouseMove(dx, dy) },
                    onMouseClick = { x, y -> viewModel.sendMouseClick(x, y) }
                )
                RemoteTab.KEYBOARD -> KeyboardScreen(
                    onSendText = { viewModel.sendText(it) },
                    onCommand = { viewModel.sendCommand(it) }
                )
                RemoteTab.DEVICES -> DevicesScreen(
                    discoveredTvs = discoveredTvs,
                    savedTvs = savedTvs,
                    isScanning = isScanning,
                    manualIp = manualIp,
                    selectedBrand = selectedBrand,
                    activeConnectedDevice = activeDevice,
                    onScanClick = { viewModel.scanNetwork() },
                    onConnectDevice = { viewModel.connectToDevice(it) },
                    onManualIpChange = { viewModel.updateManualIp(it) },
                    onSelectBrandChange = { viewModel.updateSelectedBrand(it) },
                    onConnectManualIp = { viewModel.connectManualIp() },
                    onDeleteSavedTv = { viewModel.deleteSavedTv(it) }
                )
                RemoteTab.AI_VOICE -> AiAssistantScreen(
                    chatLog = chatLog,
                    isListening = isListening,
                    isProcessingAi = isProcessingAi,
                    spokenText = spokenText,
                    onSendAiPrompt = { viewModel.processAiCommand(it) },
                    onStartVoice = { viewModel.startVoiceListening() }
                )
            }
        }
    }
}
