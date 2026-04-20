package com.kidpod.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidpod.app.R
import com.kidpod.app.domain.models.OperatingMode
import com.kidpod.app.ui.components.PinDialog
import com.kidpod.app.ui.viewmodels.ParentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(
    viewModel: ParentViewModel,
    onExit: () -> Unit,
    onExitKiosk: () -> Unit,
    isDeviceAdminActive: Boolean,
    isLockTaskActive: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (!uiState.isAuthenticated) {
        PinDialog(
            title = stringResource(R.string.parent_enter_pin),
            errorMessage = uiState.pinError,
            onConfirm = { pin -> viewModel.authenticate(pin) },
            onDismiss = onExit
        )
        return
    }

    if (uiState.showChangePinDialog) {
        ChangePinFlow(
            error = uiState.pinError,
            onComplete = { newPin, confirm -> viewModel.setPin(newPin, confirm) },
            onDismiss = { viewModel.dismissChangePinDialog() }
        )
    }

    if (uiState.showResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetConfirm() },
            title = { Text("Reset KidPod Settings?") },
            text = { Text(stringResource(R.string.parent_reset_confirm)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAllSettings(); onExit() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.parent_reset_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetConfirm() }) {
                    Text(stringResource(R.string.parent_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parent_mode_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.lock(); onExit() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit parent mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Security Status
            SettingsSection(title = "Lockdown Status") {
                StatusRow(
                    icon = Icons.Default.AdminPanelSettings,
                    label = "Device Admin",
                    value = if (isDeviceAdminActive) "Active" else "Inactive",
                    isActive = isDeviceAdminActive
                )
                StatusRow(
                    icon = Icons.Default.Lock,
                    label = "Kiosk Mode",
                    value = if (isLockTaskActive) "Active" else "Inactive",
                    isActive = isLockTaskActive
                )
            }

            // Operating Mode
            SettingsSection(title = stringResource(R.string.parent_operating_mode)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        val isOffline = uiState.settings.operatingMode == OperatingMode.OFFLINE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isOffline) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                if (isOffline) stringResource(R.string.parent_mode_offline)
                                else stringResource(R.string.parent_mode_whitelist),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            if (isOffline) "No internet — local content only"
                            else "Selected apps can use internet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = uiState.settings.operatingMode == OperatingMode.WHITELIST,
                        onCheckedChange = { checked ->
                            viewModel.setOperatingMode(
                                if (checked) OperatingMode.WHITELIST else OperatingMode.OFFLINE
                            )
                        }
                    )
                }
            }

            // Volume Limit
            SettingsSection(title = stringResource(R.string.parent_volume_limit)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("${uiState.settings.maxVolumePercent}%", style = MaterialTheme.typography.titleMedium)
                }
                Slider(
                    value = uiState.settings.maxVolumePercent.toFloat(),
                    onValueChange = { viewModel.setMaxVolume(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 17,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Actions
            SettingsSection(title = "Actions") {
                OutlinedButton(
                    onClick = { viewModel.requestChangePinDialog() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Password, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(R.string.parent_change_pin))
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExitKiosk,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(R.string.parent_exit_kiosk))
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.requestResetConfirm() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(R.string.parent_reset_settings))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
            Spacer(Modifier.padding(4.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ChangePinFlow(
    error: String?,
    onComplete: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var firstPin by remember { mutableStateOf<String?>(null) }

    if (firstPin == null) {
        PinDialog(
            title = "New PIN",
            errorMessage = error,
            onConfirm = { pin -> firstPin = pin },
            onDismiss = onDismiss
        )
    } else {
        PinDialog(
            title = "Confirm New PIN",
            errorMessage = error,
            onConfirm = { pin -> onComplete(firstPin!!, pin) },
            onDismiss = onDismiss
        )
    }
}
