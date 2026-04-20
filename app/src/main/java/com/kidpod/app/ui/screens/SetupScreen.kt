package com.kidpod.app.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidpod.app.receiver.DeviceAdminReceiver
import com.kidpod.app.ui.components.PinDialog
import com.kidpod.app.ui.viewmodels.SetupStep
import com.kidpod.app.ui.viewmodels.SetupViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.step) {
        viewModel.checkAdminStatus(context)
    }

    when (uiState.step) {
        SetupStep.WELCOME -> SetupStepScreen(
            icon = Icons.Default.MusicNote,
            title = "Welcome to KidPod!",
            description = "Turn this phone into a safe, distraction-free music and audiobook player for your child.\n\nNo social media. No games. No browser. Just music.",
            buttonText = "Get Started",
            onNext = { viewModel.proceedToNextStep(context) }
        )

        SetupStep.DEVICE_ADMIN -> DeviceAdminStep(
            isAdminActive = uiState.isAdminActive,
            onActivate = {
                val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "KidPod needs Device Admin to prevent children from bypassing the lockdown."
                    )
                }
                context.startActivity(intent)
            },
            onCheckAndContinue = {
                viewModel.checkAdminStatus(context)
                if (uiState.isAdminActive) viewModel.proceedToNextStep(context)
            }
        )

        SetupStep.PIN_CREATE -> PinCreateStep(
            error = uiState.pinError,
            onPinEntered = { pin ->
                viewModel.setPinFirst(pin)
                viewModel.proceedToNextStep(context)
            }
        )

        SetupStep.PIN_CONFIRM -> PinConfirmStep(
            error = uiState.pinError,
            onPinEntered = { pin -> viewModel.confirmPin(pin) }
        )

        SetupStep.COMPLETE -> SetupStepScreen(
            icon = Icons.Default.CheckCircle,
            title = "All Set!",
            description = "KidPod is ready.\n\nConnect via USB to add music and audiobooks to:\n  /KidPod/Music/\n  /KidPod/Audiobooks/\n\nLong-press the KidPod logo for 3 seconds to access Parent Settings.",
            buttonText = "Start KidPod",
            onNext = {
                viewModel.completeSetup()
                onSetupComplete()
            }
        )
    }
}

@Composable
private fun SetupStepScreen(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(buttonText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DeviceAdminStep(
    isAdminActive: Boolean,
    onActivate: () -> Unit,
    onCheckAndContinue: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isAdminActive) Icons.Default.CheckCircle else Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = if (isAdminActive) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Device Admin",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isAdminActive) {
                    "Device Admin is active. KidPod can now prevent uninstallation and enforce lockdown."
                } else {
                    "KidPod needs Device Admin permission to:\n\n• Prevent uninstallation\n• Block launcher changes\n• Enforce kiosk mode\n\nTap Activate and approve when prompted."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(48.dp))

            if (!isAdminActive) {
                Button(
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Activate Device Admin", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = onCheckAndContinue,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdminActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    if (isAdminActive) "Continue" else "I activated it — check & continue",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PinCreateStep(error: String?, onPinEntered: (String) -> Unit) {
    PinDialog(
        title = "Create Parent PIN",
        errorMessage = error,
        onConfirm = onPinEntered,
        onDismiss = {}  // Cannot dismiss during setup
    )
}

@Composable
private fun PinConfirmStep(error: String?, onPinEntered: (String) -> Unit) {
    PinDialog(
        title = "Confirm Your PIN",
        errorMessage = error,
        onConfirm = onPinEntered,
        onDismiss = {}  // Cannot dismiss during setup
    )
}
