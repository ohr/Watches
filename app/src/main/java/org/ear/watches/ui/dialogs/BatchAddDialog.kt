package org.ear.watches.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ear.watches.R
import org.ear.watches.ui.theme.WatchesTheme

private enum class BatchAddMode {
    ROSTER,
    NUMBERED
}

/**
 * A dialog allowing the user to instantiate multiple stopwatches at once either from
 * a saved athlete roster or with a common numeric prefix.
 */
@Composable
fun BatchAddDialog(
    proposedAthleteNames: List<String> = emptyList(),
    onConfirmBatchWithNames: (names: List<String>) -> Unit = {},
    onConfirmBatchNumbered: (count: Int, namePrefix: String) -> Unit = { _, _ -> },
    onRemoveProposedName: (name: String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var mode by remember(proposedAthleteNames.isNotEmpty()) {
        mutableStateOf(if (proposedAthleteNames.isNotEmpty()) BatchAddMode.ROSTER else BatchAddMode.NUMBERED)
    }

    LaunchedEffect(proposedAthleteNames) {
        if (proposedAthleteNames.isEmpty()) {
            mode = BatchAddMode.NUMBERED
        }
    }

    var selectedNames by remember(proposedAthleteNames) {
        mutableStateOf(proposedAthleteNames.toSet())
    }

    var count by remember { mutableIntStateOf(4) }
    val defaultPrefix = stringResource(R.string.default_athlete_prefix)
    var prefix by remember(defaultPrefix) { mutableStateOf(defaultPrefix) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Fixed Top Header
                Text(
                    text = stringResource(R.string.dialog_batch_add_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (proposedAthleteNames.isNotEmpty()) {
                    TabRow(
                        selectedTabIndex = mode.ordinal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Tab(
                            selected = mode == BatchAddMode.ROSTER,
                            onClick = { mode = BatchAddMode.ROSTER },
                            text = { Text(stringResource(R.string.dialog_batch_add_saved_roster)) }
                        )
                        Tab(
                            selected = mode == BatchAddMode.NUMBERED,
                            onClick = { mode = BatchAddMode.NUMBERED },
                            text = { Text(stringResource(R.string.dialog_batch_add_count_label)) }
                        )
                    }
                }

                if (mode == BatchAddMode.ROSTER && proposedAthleteNames.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedNames = proposedAthleteNames.toSet() }
                        ) {
                            Text(stringResource(R.string.btn_select_all))
                        }
                        TextButton(
                            onClick = { selectedNames = emptySet() }
                        ) {
                            Text(stringResource(R.string.btn_deselect_all))
                        }
                    }

                    // Scrollable Middle Section
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = proposedAthleteNames,
                            key = { it }
                        ) { name ->
                            val isChecked = name in selectedNames
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNames = if (isChecked) {
                                            selectedNames - name
                                        } else {
                                            selectedNames + name
                                        }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedNames = if (checked) {
                                            selectedNames + name
                                        } else {
                                            selectedNames - name
                                        }
                                    }
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )
                                IconButton(
                                    onClick = { onRemoveProposedName(name) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_remove_proposed_name),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Scrollable Middle Section for Numbered Mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text(stringResource(R.string.dialog_batch_add_prefix_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = stringResource(R.string.dialog_batch_add_number_athletes),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (count > 1) count-- },
                                enabled = count > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.cd_decrease_count),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }

                            IconButton(
                                onClick = { if (count < 12) count++ },
                                enabled = count < 12
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.cd_increase_count),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Fixed Bottom Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (mode == BatchAddMode.ROSTER && proposedAthleteNames.isNotEmpty()) {
                        Button(
                            onClick = {
                                val namesToAdd = proposedAthleteNames.filter { it in selectedNames }
                                onConfirmBatchWithNames(namesToAdd)
                            },
                            enabled = selectedNames.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(stringResource(R.string.btn_add_selected_count, selectedNames.size))
                        }
                    } else {
                        Button(
                            onClick = {
                                val finalPrefix = prefix.ifBlank { defaultPrefix }
                                onConfirmBatchNumbered(count, finalPrefix)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(stringResource(R.string.dialog_batch_add_confirm, count))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchAddDialog(
    onConfirm: (count: Int, namePrefix: String) -> Unit,
    onDismiss: () -> Unit
) {
    BatchAddDialog(
        proposedAthleteNames = emptyList(),
        onConfirmBatchWithNames = {},
        onConfirmBatchNumbered = onConfirm,
        onRemoveProposedName = {},
        onDismiss = onDismiss
    )
}

@Preview
@Composable
fun BatchAddDialogPreview() {
    WatchesTheme {
        BatchAddDialog(
            proposedAthleteNames = listOf("Alice", "Bob", "Charlie"),
            onConfirmBatchWithNames = {},
            onConfirmBatchNumbered = { _, _ -> },
            onRemoveProposedName = {},
            onDismiss = {}
        )
    }
}
