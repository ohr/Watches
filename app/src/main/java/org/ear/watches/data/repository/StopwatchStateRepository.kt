package org.ear.watches.data.repository

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.ear.watches.data.model.PersistedStopwatchState
import org.ear.watches.data.util.SystemTimeProvider
import org.ear.watches.data.util.TimeProvider

/**
 * Storage abstraction for the timing state, so the ViewModel can be tested with an in-memory fake.
 */
interface StopwatchStateStore {
    /** Loads the last saved state, already reconciled for the current boot, or `null` if none exists. */
    suspend fun load(): PersistedStopwatchState?

    /** Persists the given state, replacing any previous one. */
    suspend fun save(state: PersistedStopwatchState)
}

private val Context.stopwatchStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "stopwatch_state")

/**
 * [StopwatchStateStore] backed by a dedicated Preferences DataStore holding the state as one JSON blob.
 * Kept separate from the settings DataStore so frequent timing writes never touch user preferences.
 */
class StopwatchStateRepository(
    private val dataStore: DataStore<Preferences>,
    private val bootCountProvider: () -> Int?,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : StopwatchStateStore {

    constructor(context: Context) : this(
        dataStore = context.applicationContext.stopwatchStateDataStore,
        bootCountProvider = { readBootCount(context.applicationContext) }
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun load(): PersistedStopwatchState? {
        val raw = runCatching { dataStore.data.first()[STATE_JSON] }
            .onFailure { Log.w(TAG, "Failed to read stopwatch state", it) }
            .getOrNull() ?: return null
        val decoded = runCatching { json.decodeFromString<PersistedStopwatchState>(raw) }
            .onFailure { Log.w(TAG, "Discarding unreadable stopwatch state", it) }
            .getOrNull() ?: return null
        return decoded.reconcile(bootCountProvider(), timeProvider.currentTimeMillis())
    }

    override suspend fun save(state: PersistedStopwatchState) {
        val stamped = state.copy(
            bootCount = bootCountProvider(),
            savedAtMillis = timeProvider.currentTimeMillis()
        )
        val encoded = json.encodeToString(PersistedStopwatchState.serializer(), stamped)
        runCatching { dataStore.edit { it[STATE_JSON] = encoded } }
            .onFailure { Log.w(TAG, "Failed to save stopwatch state", it) }
    }

    companion object {
        private const val TAG = "StopwatchStateRepo"
        val STATE_JSON = stringPreferencesKey("stopwatch_state_json")

        fun readBootCount(context: Context): Int? =
            runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
                .getOrNull()
    }
}
