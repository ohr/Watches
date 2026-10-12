package com.ear.watches.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ear.watches.R
import com.ear.watches.data.model.AppFontSize
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.AppThemeMode
import com.ear.watches.ui.theme.WatchesTheme

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onToggleSoundHaptics: () -> Unit,
    onToggleHardwareKeys: () -> Unit,
    onToggleAutoSort: () -> Unit,
    onSetPrecision: (AppPrecision) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetFontSizeScale: (AppFontSize) -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_settings_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Digital Time Precision Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.setting_time_precision),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        AppPrecision.entries.forEach { precision ->
                            val label = when (precision) {
                                AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
                                AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
                                AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (settings.timePrecision == precision),
                                        onClick = { onSetPrecision(precision) },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (settings.timePrecision == precision),
                                    onClick = null
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Theme Mode Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.setting_theme_mode),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        AppThemeMode.entries.forEach { themeMode ->
                            val label = when (themeMode) {
                                AppThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system_default)
                                AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (settings.themeMode == themeMode),
                                        onClick = { onSetThemeMode(themeMode) },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (settings.themeMode == themeMode),
                                    onClick = null
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Font Size Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.setting_font_size),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        AppFontSize.entries.forEach { fontSize ->
                            val label = when (fontSize) {
                                AppFontSize.SMALL -> stringResource(R.string.font_size_small)
                                AppFontSize.NORMAL -> stringResource(R.string.font_size_normal)
                                AppFontSize.LARGE -> stringResource(R.string.font_size_large)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (settings.fontSizeScale == fontSize),
                                        onClick = { onSetFontSizeScale(fontSize) },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (settings.fontSizeScale == fontSize),
                                    onClick = null
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Language Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.setting_language),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        AppLanguage.entries.forEach { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (settings.language == language),
                                        onClick = { onSetLanguage(language) },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (settings.language == language),
                                    onClick = null
                                )
                                Text(
                                    text = stringResource(language.labelResId),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Sound & Haptic Feedback Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSoundHaptics() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_sound_haptics),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.setting_sound_haptics_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.soundAndHapticsEnabled,
                        onCheckedChange = { onToggleSoundHaptics() }
                    )
                }

                HorizontalDivider()

                // Hardware Volume Keys Control Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHardwareKeys() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_hardware_keys),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.setting_hardware_keys_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.hardwareVolumeKeysEnabled,
                        onCheckedChange = { onToggleHardwareKeys() }
                    )
                }

                HorizontalDivider()

                // Auto-Sort Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAutoSort() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_auto_sort),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Switch(
                        checked = settings.autoSortEnabled,
                        onCheckedChange = { onToggleAutoSort() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.btn_done))
            }
        }
    )
}

@Preview
@Composable
fun SettingsDialogPreview() {
    WatchesTheme {
        SettingsDialog(
            settings = AppSettings(),
            onToggleSoundHaptics = {},
            onToggleHardwareKeys = {},
            onToggleAutoSort = {},
            onSetPrecision = {},
            onSetLanguage = {},
            onSetThemeMode = {},
            onSetFontSizeScale = {},
            onDismiss = {}
        )
    }
}
