package com.ear.watches.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.ear.watches.R
import com.ear.watches.data.model.AppFontSize
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.AppThemeMode
import com.ear.watches.ui.theme.WatchesTheme

/**
 * A modal dialog presenting application-wide configuration options.
 * Allows users to toggle haptics, volume keys, display precision, auto-sort, and language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    onToggleSoundHaptics: () -> Unit,
    onToggleHardwareKeys: () -> Unit,
    onToggleAutoSort: () -> Unit,
    onSetPrecision: (AppPrecision) -> Unit,
    onSetRunningDisplayPrecision: (AppPrecision) -> Unit = {},
    onSetLanguage: (AppLanguage) -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit = onSetLanguage,
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
                // 1. Running Display Precision Dropdown
                var runningPrecisionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = runningPrecisionExpanded,
                    onExpandedChange = { runningPrecisionExpanded = !runningPrecisionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        value = when (settings.runningDisplayPrecision) {
                            AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
                            AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
                            AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
                        },
                        onValueChange = {},
                        label = { Text(text = stringResource(R.string.setting_running_display_precision)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = runningPrecisionExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = runningPrecisionExpanded,
                        onDismissRequest = { runningPrecisionExpanded = false }
                    ) {
                        AppPrecision.entries.forEach { precision ->
                            val label = when (precision) {
                                AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
                                AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
                                AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
                            }
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    onSetRunningDisplayPrecision(precision)
                                    runningPrecisionExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // 2. Result & Lap Precision Dropdown
                var timePrecisionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = timePrecisionExpanded,
                    onExpandedChange = { timePrecisionExpanded = !timePrecisionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        value = when (settings.timePrecision) {
                            AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
                            AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
                            AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
                        },
                        onValueChange = {},
                        label = { Text(text = stringResource(R.string.setting_time_precision)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = timePrecisionExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = timePrecisionExpanded,
                        onDismissRequest = { timePrecisionExpanded = false }
                    ) {
                        AppPrecision.entries.forEach { precision ->
                            val label = when (precision) {
                                AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
                                AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
                                AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
                            }
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    onSetPrecision(precision)
                                    timePrecisionExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // 3. Theme Mode Dropdown
                var themeModeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = themeModeExpanded,
                    onExpandedChange = { themeModeExpanded = !themeModeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        value = when (settings.themeMode) {
                            AppThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system_default)
                            AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                        },
                        onValueChange = {},
                        label = { Text(text = stringResource(R.string.setting_theme_mode)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeModeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = themeModeExpanded,
                        onDismissRequest = { themeModeExpanded = false }
                    ) {
                        AppThemeMode.entries.forEach { themeMode ->
                            val label = when (themeMode) {
                                AppThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system_default)
                                AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    onSetThemeMode(themeMode)
                                    themeModeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // 4. Font Size Scale Dropdown
                var fontSizeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = fontSizeExpanded,
                    onExpandedChange = { fontSizeExpanded = !fontSizeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        value = when (settings.fontSizeScale) {
                            AppFontSize.SMALL -> stringResource(R.string.font_size_small)
                            AppFontSize.NORMAL -> stringResource(R.string.font_size_normal)
                            AppFontSize.LARGE -> stringResource(R.string.font_size_large)
                        },
                        onValueChange = {},
                        label = { Text(text = stringResource(R.string.setting_font_size)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontSizeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = fontSizeExpanded,
                        onDismissRequest = { fontSizeExpanded = false }
                    ) {
                        AppFontSize.entries.forEach { fontSize ->
                            val label = when (fontSize) {
                                AppFontSize.SMALL -> stringResource(R.string.font_size_small)
                                AppFontSize.NORMAL -> stringResource(R.string.font_size_normal)
                                AppFontSize.LARGE -> stringResource(R.string.font_size_large)
                            }
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    onSetFontSizeScale(fontSize)
                                    fontSizeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // 5. Language Dropdown
                var languageMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = languageMenuExpanded,
                    onExpandedChange = { languageMenuExpanded = !languageMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        value = stringResource(settings.language.labelResId),
                        onValueChange = {},
                        label = { Text(text = stringResource(R.string.setting_language)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(language.labelResId)) },
                                onClick = {
                                    onLanguageSelected(language)
                                    onSetLanguage(language)
                                    languageMenuExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
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
            onSetRunningDisplayPrecision = {},
            onSetLanguage = {},
            onSetThemeMode = {},
            onSetFontSizeScale = {},
            onDismiss = {}
        )
    }
}
