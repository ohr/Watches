package org.ear.watches.ui.dialogs

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.ear.watches.R
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import org.ear.watches.ui.theme.WatchesTheme

/**
 * A modal dialog presenting application-wide configuration options.
 * Allows users to toggle haptics, volume keys, display precision, auto-sort, and language.
 */
@Composable
fun SettingsDialog(
    settings: AppSettings,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleHardwareKeys: () -> Unit,
    onSetAutoSortOption: (AppAutoSortOption) -> Unit = {},
    onToggleAutoSort: () -> Unit = {},
    onToggleKeepScreenOn: () -> Unit = {},
    onToggleDynamicColor: () -> Unit = {},
    onSetPrecision: (AppPrecision) -> Unit,
    onSetRunningDisplayPrecision: (AppPrecision) -> Unit = {},
    onSetLanguage: (AppLanguage) -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit = onSetLanguage,
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetFontSizeScale: (AppFontSize) -> Unit = {},
    onResetSettingsToDefault: () -> Unit = {},
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
                SettingDropdown(
                    label = stringResource(R.string.setting_running_display_precision),
                    selectedLabel = precisionLabel(settings.runningDisplayPrecision),
                    options = AppPrecision.entries,
                    optionLabel = { precisionLabel(it) },
                    onOptionSelected = onSetRunningDisplayPrecision
                )

                // 2. Result & Lap Precision Dropdown
                SettingDropdown(
                    label = stringResource(R.string.setting_time_precision),
                    selectedLabel = precisionLabel(settings.timePrecision),
                    options = AppPrecision.entries,
                    optionLabel = { precisionLabel(it) },
                    onOptionSelected = onSetPrecision
                )

                // 3. Theme Mode Dropdown
                SettingDropdown(
                    label = stringResource(R.string.setting_theme_mode),
                    selectedLabel = themeModeLabel(settings.themeMode),
                    options = AppThemeMode.entries,
                    optionLabel = { themeModeLabel(it) },
                    onOptionSelected = onSetThemeMode
                )

                // 4. Font Size Scale Dropdown
                SettingDropdown(
                    label = stringResource(R.string.setting_font_size),
                    selectedLabel = fontSizeLabel(settings.fontSizeScale),
                    options = AppFontSize.entries,
                    optionLabel = { fontSizeLabel(it) },
                    onOptionSelected = onSetFontSizeScale
                )

                // 5. Language Dropdown
                SettingDropdown(
                    label = stringResource(R.string.setting_language),
                    selectedLabel = stringResource(settings.language.labelResId),
                    options = AppLanguage.entries,
                    optionLabel = { stringResource(it.labelResId) },
                    onOptionSelected = { language ->
                        onLanguageSelected(language)
                        onSetLanguage(language)
                    }
                )

                HorizontalDivider()

                // Dynamic Colors Switch (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingSwitchRow(
                        title = stringResource(R.string.setting_dynamic_color),
                        description = stringResource(R.string.setting_dynamic_color_desc),
                        checked = settings.useDynamicColor,
                        onToggle = onToggleDynamicColor
                    )

                    HorizontalDivider()
                }

                // Sound Cues Switch
                SettingSwitchRow(
                    title = stringResource(R.string.setting_sound),
                    description = stringResource(R.string.setting_sound_desc),
                    checked = settings.soundEnabled,
                    onToggle = onToggleSound
                )

                HorizontalDivider()

                // Haptic Vibration Switch
                SettingSwitchRow(
                    title = stringResource(R.string.setting_haptics),
                    description = stringResource(R.string.setting_haptics_desc),
                    checked = settings.hapticsEnabled,
                    onToggle = onToggleHaptics
                )

                HorizontalDivider()

                // Hardware Volume Keys Control Switch
                SettingSwitchRow(
                    title = stringResource(R.string.setting_hardware_keys),
                    description = stringResource(R.string.setting_hardware_keys_desc),
                    checked = settings.hardwareVolumeKeysEnabled,
                    onToggle = onToggleHardwareKeys
                )

                HorizontalDivider()

                // Auto-Sort Dropdown
                SettingDropdown(
                    label = stringResource(R.string.setting_auto_sort),
                    selectedLabel = autoSortLabel(settings.autoSortOption),
                    options = AppAutoSortOption.entries,
                    optionLabel = { autoSortLabel(it) },
                    onOptionSelected = onSetAutoSortOption
                )

                HorizontalDivider()

                // Keep Screen Awake Switch
                SettingSwitchRow(
                    title = stringResource(R.string.setting_keep_screen_on),
                    description = stringResource(R.string.setting_keep_screen_on_desc),
                    checked = settings.keepScreenOnWhileRunning,
                    onToggle = onToggleKeepScreenOn
                )

                HorizontalDivider()

                // Reset Settings to Default Button
                OutlinedButton(
                    onClick = { onResetSettingsToDefault() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_reset_settings_default),
                        style = MaterialTheme.typography.bodyMedium
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingDropdown(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            label = { Text(text = label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun precisionLabel(precision: AppPrecision): String = when (precision) {
    AppPrecision.TENTHS -> stringResource(R.string.precision_tenths)
    AppPrecision.HUNDREDTHS -> stringResource(R.string.precision_hundredths)
    AppPrecision.SECONDS -> stringResource(R.string.precision_seconds)
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.SYSTEM_DEFAULT -> stringResource(R.string.theme_system_default)
    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun fontSizeLabel(fontSize: AppFontSize): String = when (fontSize) {
    AppFontSize.SMALL -> stringResource(R.string.font_size_small)
    AppFontSize.NORMAL -> stringResource(R.string.font_size_normal)
    AppFontSize.LARGE -> stringResource(R.string.font_size_large)
}

@Composable
private fun autoSortLabel(option: AppAutoSortOption): String = when (option) {
    AppAutoSortOption.NONE -> stringResource(R.string.auto_sort_none)
    AppAutoSortOption.BY_FASTEST_LAP -> stringResource(R.string.auto_sort_fastest_lap)
    AppAutoSortOption.BY_NAME -> stringResource(R.string.auto_sort_by_name)
}

@Preview
@Composable
fun SettingsDialogPreview() {
    WatchesTheme {
        SettingsDialog(
            settings = AppSettings(),
            onToggleSound = {},
            onToggleHaptics = {},
            onToggleHardwareKeys = {},
            onSetAutoSortOption = {},
            onToggleAutoSort = {},
            onToggleKeepScreenOn = {},
            onToggleDynamicColor = {},
            onSetPrecision = {},
            onSetRunningDisplayPrecision = {},
            onSetLanguage = {},
            onSetThemeMode = {},
            onSetFontSizeScale = {},
            onResetSettingsToDefault = {},
            onDismiss = {}
        )
    }
}
