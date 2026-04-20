package com.kidpod.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kidpod.app.R
import com.kidpod.app.utils.Constants

@Composable
fun PinDialog(
    title: String,
    errorMessage: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // PIN dots display
                PinDots(pinLength = pin.length, maxLength = Constants.MAX_PIN_LENGTH)

                Spacer(Modifier.height(8.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))

                // Number pad
                NumberPad(
                    onDigit = { digit ->
                        if (pin.length < Constants.MAX_PIN_LENGTH) {
                            pin += digit
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    },
                    onConfirm = {
                        if (pin.length >= Constants.MIN_PIN_LENGTH) {
                            onConfirm(pin)
                            pin = ""
                        }
                    },
                    confirmEnabled = pin.length >= Constants.MIN_PIN_LENGTH
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.parent_cancel))
                }
            }
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, maxLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val filled = index < pinLength
            Surface(
                modifier = Modifier.size(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (filled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                }
            ) {}
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.weight(1f))
                        "⌫" -> IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .weight(1f)
                                .size(64.dp)
                                .semantics { contentDescription = "Backspace" }
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = null)
                        }
                        else -> FilledTonalButton(
                            onClick = { onDigit(key) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(key, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Unlock", style = MaterialTheme.typography.labelLarge)
        }
    }
}
