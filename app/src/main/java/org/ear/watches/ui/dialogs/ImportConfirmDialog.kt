package org.ear.watches.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ear.watches.R
import org.ear.watches.ui.theme.WatchesTheme

/**
 * Dialog prompting the user to decide whether to replace existing stopwatches or append
 * the imported stopwatches from a .watches.json backup file.
 */
@Composable
fun ImportConfirmDialog(
    stopwatchCount: Int,
    hasExistingWatches: Boolean,
    onReplaceAll: () -> Unit,
    onAppend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.import_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.import_dialog_message, stopwatchCount),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasExistingWatches) {
                    OutlinedButton(onClick = onAppend) {
                        Text(stringResource(R.string.btn_append))
                    }
                }
                Button(onClick = onReplaceAll) {
                    Text(
                        stringResource(
                            if (hasExistingWatches) R.string.btn_replace_all else R.string.action_import
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview
@Composable
fun ImportConfirmDialogPreview() {
    WatchesTheme {
        ImportConfirmDialog(
            stopwatchCount = 3,
            hasExistingWatches = true,
            onReplaceAll = {},
            onAppend = {},
            onDismiss = {}
        )
    }
}
