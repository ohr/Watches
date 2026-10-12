package com.ear.watches

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.repository.SettingsRepository
import com.ear.watches.ui.screens.MainStopwatchScreen
import com.ear.watches.ui.theme.WatchesTheme
import com.ear.watches.ui.viewmodel.StopwatchViewModel
import com.ear.watches.util.FeedbackManager
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: StopwatchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StopwatchViewModel(
                    settingsRepository = SettingsRepository(applicationContext)
                ) as T
            }
        }
    }
    private lateinit var feedbackManager: FeedbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        feedbackManager = FeedbackManager(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.settings.language) {
                updateAppLocale(uiState.settings.language)
            }

            WatchesTheme(
                themeMode = uiState.settings.themeMode,
                fontSizeScale = uiState.settings.fontSizeScale
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainStopwatchScreen(
                        viewModel = viewModel,
                        feedbackManager = feedbackManager,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::feedbackManager.isInitialized) {
            feedbackManager.release()
        }
    }

    private fun updateAppLocale(language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LOCALE_SERVICE) as? LocaleManager
            val localeList = if (language.languageTag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language.languageTag)
            }
            localeManager?.applicationLocales = localeList
        } else {
            val locale = if (language.languageTag.isEmpty()) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(language.languageTag)
            }
            val config = resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = viewModel.uiState.value.settings

        if (settings.hardwareVolumeKeysEnabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event?.repeatCount == 0) {
                        viewModel.handleVolumeUp()
                        feedbackManager.playFeedback(settings.soundAndHapticsEnabled)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event?.repeatCount == 0) {
                        viewModel.handleVolumeDown()
                        feedbackManager.playFeedback(settings.soundAndHapticsEnabled)
                    }
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = viewModel.uiState.value.settings

        if (settings.hardwareVolumeKeysEnabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    return true
                }
            }
        }

        return super.onKeyUp(keyCode, event)
    }
}
