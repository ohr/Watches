package org.ear.watches

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.data.repository.StopwatchStateRepository
import org.ear.watches.service.StopwatchService
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.ear.watches.util.AppCompatLocaleController

/**
 * Application container. Owns the single, process-wide [StopwatchViewModel] so that the UI and
 * [StopwatchService] (notification actions) operate on the same timing state, even after the
 * activity has been destroyed while watches keep running.
 */
class WatchesApplication : Application(), ViewModelStoreOwner {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    val stopwatchViewModel: StopwatchViewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory {
                initializer {
                    StopwatchViewModel(
                        settingsRepository = SettingsRepository(this@WatchesApplication),
                        stateStore = StopwatchStateRepository(this@WatchesApplication),
                        localeController = AppCompatLocaleController()
                    )
                }
            }
        )[StopwatchViewModel::class.java]
    }

    override fun onCreate() {
        super.onCreate()
        val viewModel = stopwatchViewModel
        // Run the foreground service whenever at least one watch is running (unless disabled).
        // The service stops itself when this no longer holds.
        viewModel.viewModelScope.launch {
            viewModel.uiState
                .map { state -> state.settings.showRunningNotification && state.stopwatches.any { it.isRunning } }
                .distinctUntilChanged()
                .filter { shouldRun -> shouldRun }
                .collect { StopwatchService.start(this@WatchesApplication) }
        }
    }
}
