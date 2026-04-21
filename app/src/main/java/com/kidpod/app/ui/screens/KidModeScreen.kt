package com.kidpod.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidpod.app.R
import com.kidpod.app.ui.components.PinDialog
import com.kidpod.app.ui.theme.Coral
import com.kidpod.app.ui.theme.SkyBlue
import com.kidpod.app.ui.theme.SoftPurple
import com.kidpod.app.ui.theme.Teal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KidModeScreen(
    onAuthenticatedAsParent: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onMusicTap: () -> Unit = {},
    onAudiobooksTap: () -> Unit = {},
    onPlaylistsTap: () -> Unit = {}
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val wrongPinText = stringResource(R.string.parent_pin_wrong)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo — long-press 3 seconds to open parent mode
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            pinError = null
                            showPinDialog = true
                        }
                    )
                    .semantics { contentDescription = "KidPod logo — long press for parent settings" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "KidPod",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(48.dp))

            // Navigation cards
            MediaCard(
                icon = Icons.Default.LibraryMusic,
                label = stringResource(R.string.kid_mode_music),
                color = Coral,
                modifier = Modifier.fillMaxWidth(),
                onClick = onMusicTap
            )

            Spacer(Modifier.height(16.dp))

            MediaCard(
                icon = Icons.Default.Headphones,
                label = stringResource(R.string.kid_mode_audiobooks),
                color = Teal,
                modifier = Modifier.fillMaxWidth(),
                onClick = onAudiobooksTap
            )

            Spacer(Modifier.height(16.dp))

            MediaCard(
                icon = Icons.Default.PlaylistPlay,
                label = stringResource(R.string.kid_mode_playlists),
                color = SoftPurple,
                modifier = Modifier.fillMaxWidth(),
                onClick = onPlaylistsTap
            )
        }
    }

    if (showPinDialog) {
        PinDialog(
            title = stringResource(R.string.parent_enter_pin),
            errorMessage = pinError,
            onConfirm = { pin ->
                if (onVerifyPin(pin)) {
                    showPinDialog = false
                    onAuthenticatedAsParent()
                } else {
                    pinError = wrongPinText
                }
            },
            onDismiss = {
                showPinDialog = false
                pinError = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaCard(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(Modifier.size(20.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
