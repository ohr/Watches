package org.ear.watches.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ear.watches.R
import org.ear.watches.ui.theme.WatchesTheme

/**
 * A dialog enabling the user to assign a custom session name or pick from saved dynamic proposed session names.
 */
@Composable
fun NameSessionDialog(
    initialName: String = "",
    proposedSessionNames: List<String> = emptyList(),
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit,
    onRemoveProposedSessionName: ((String) -> Unit)? = null
) {
    var sessionNameText by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_name_session_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_name_session_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (proposedSessionNames.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_name_session_proposed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Dynamic proposed session chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            proposedSessionNames.forEach { proposal ->
                                FilterChip(
                                    selected = (sessionNameText == proposal),
                                    onClick = { sessionNameText = proposal },
                                    label = { Text(proposal) },
                                    trailingIcon = if (onRemoveProposedSessionName != null) {
                                        {
                                            IconButton(
                                                onClick = { onRemoveProposedSessionName(proposal) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.cd_remove_proposed_session),
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Custom input field
                OutlinedTextField(
                    value = sessionNameText,
                    onValueChange = { sessionNameText = it },
                    label = { Text(stringResource(R.string.dialog_name_session_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(sessionNameText.trim())
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview
@Composable
fun NameSessionDialogPreview() {
    WatchesTheme {
        NameSessionDialog(
            initialName = "Heat 1",
            proposedSessionNames = listOf("Warm-up", "Heat 1", "Heat 2", "Finals"),
            onConfirm = {},
            onDismiss = {},
            onRemoveProposedSessionName = {}
        )
    }
}
