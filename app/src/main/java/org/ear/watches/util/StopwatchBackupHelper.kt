package org.ear.watches.util

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchBackupPayload
import org.ear.watches.data.model.StopwatchItem
import java.io.InputStream
import java.util.UUID

/**
 * Utility helper for serializing and deserializing stopwatch backup payloads
 * to and from JSON format, ensuring all stopwatches are sanitized and normalized to stopped states.
 */
object StopwatchBackupHelper {

    const val MAX_BACKUP_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    const val MAX_STOPWATCH_COUNT = 100
    const val MAX_RUNS_PER_WATCH = 100
    const val MAX_LAPS_PER_RUN = 1_000
    const val MAX_STRING_LENGTH = 120
    const val MAX_APP_VERSION_LENGTH = 50
    // Maximum plausible elapsed duration: 365 days in nanoseconds (~3.15e16)
    const val MAX_REASONABLE_DURATION_NANOS = 365L * 24 * 60 * 60 * 1_000_000_000L

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes a list of [StopwatchItem]s into a formatted JSON string backup payload.
     * Guarantees that any stopwatches or active runs are marked as stopped.
     */
    fun createBackupJson(
        stopwatches: List<StopwatchItem>,
        appVersion: String? = null,
        exportedAtMillis: Long = System.currentTimeMillis(),
    ): String {
        val normalized = stopwatches.map { watch ->
            watch.copy(
                isRunning = false,
                runs = watch.runs.map { run ->
                    if (run.isRunning) {
                        run.copy(
                            endTimeNanos = run.accumulatedNanos.coerceAtLeast(1L),
                            accumulatedNanos = run.accumulatedNanos,
                        )
                    } else {
                        run
                    }
                },
            )
        }

        val payload = StopwatchBackupPayload(
            version = StopwatchBackupPayload.CURRENT_VERSION,
            exportedAtMillis = exportedAtMillis,
            appVersion = appVersion?.take(MAX_APP_VERSION_LENGTH)?.sanitizeText(),
            stopwatches = normalized,
        )

        return json.encodeToString(StopwatchBackupPayload.serializer(), payload)
    }

    /**
     * Deserializes, sanitizes, and validates a JSON string backup payload into a [StopwatchBackupPayload].
     *
     * @return [Result.success] with the sanitized payload if valid, or [Result.failure] if corrupt,
     * unsupported version, or containing malicious/out-of-bounds parameters.
     */
    fun parseBackupJson(jsonString: String): Result<StopwatchBackupPayload> {
        return runCatching {
            if (!jsonString.contains("\"stopwatches\"")) {
                throw IllegalArgumentException("Invalid Watches backup: missing stopwatches definition")
            }
            val payload = json.decodeFromString(StopwatchBackupPayload.serializer(), jsonString)
            if (payload.version <= 0 || payload.version > StopwatchBackupPayload.CURRENT_VERSION) {
                throw IllegalArgumentException("Unsupported backup version: ${payload.version}")
            }
            if (payload.stopwatches.size > MAX_STOPWATCH_COUNT) {
                throw IllegalArgumentException(
                    "Backup contains too many stopwatches (${payload.stopwatches.size} > $MAX_STOPWATCH_COUNT)",
                )
            }

            val sanitizedAppVersion = payload.appVersion?.take(MAX_APP_VERSION_LENGTH)?.sanitizeText()

            // Normalize and sanitize all watches to ensure non-running state and safe numeric/string values
            val sanitizedWatches = payload.stopwatches.mapIndexed { index, watch ->
                val fallbackColor = StopwatchItem.DEFAULT_PASTEL_COLORS[index % StopwatchItem.DEFAULT_PASTEL_COLORS.size]
                val safeColor = if (watch.pastelColorHex == 0L) {
                    fallbackColor
                } else {
                    (watch.pastelColorHex and 0x00FFFFFFL) or 0xFF000000L
                }

                val sanitizedName = watch.name
                    .sanitizeText()
                    .take(MAX_STRING_LENGTH)
                    .ifBlank { "Athlete ${index + 1}" }

                if (watch.runs.size > MAX_RUNS_PER_WATCH) {
                    throw IllegalArgumentException(
                        "Stopwatch '$sanitizedName' contains too many run sessions (${watch.runs.size} > $MAX_RUNS_PER_WATCH)",
                    )
                }

                val sanitizedRuns = watch.runs.mapIndexed { runIdx, run ->
                    if (run.laps.size > MAX_LAPS_PER_RUN) {
                        throw IllegalArgumentException(
                            "Run session ${run.runIndex} contains too many laps (${run.laps.size} > $MAX_LAPS_PER_RUN)",
                        )
                    }

                    val sanitizedRunName = run.name
                        ?.sanitizeText()
                        ?.take(MAX_STRING_LENGTH)
                        ?.takeIf { it.isNotBlank() }

                    val safeAccumulated = run.accumulatedNanos.coerceIn(0L, MAX_REASONABLE_DURATION_NANOS)
                    val safeStartTimeMillis = run.startTimeMillis.coerceAtLeast(0L)

                    val sanitizedLaps = run.laps.mapIndexed { lapIdx, lap ->
                        LapRecord(
                            lapNumber = lap.lapNumber.coerceAtLeast(lapIdx + 1),
                            lapTimeNanos = lap.lapTimeNanos.coerceIn(0L, MAX_REASONABLE_DURATION_NANOS),
                            totalTimeNanos = lap.totalTimeNanos.coerceIn(0L, MAX_REASONABLE_DURATION_NANOS),
                            lapDeltaNanos = lap.lapDeltaNanos?.coerceIn(
                                -MAX_REASONABLE_DURATION_NANOS,
                                MAX_REASONABLE_DURATION_NANOS,
                            ),
                        )
                    }

                    RunSession(
                        runIndex = run.runIndex.coerceAtLeast(runIdx + 1),
                        startTimeNanos = 0L,
                        endTimeNanos = safeAccumulated.coerceAtLeast(1L),
                        accumulatedNanos = safeAccumulated,
                        startTimeMillis = safeStartTimeMillis,
                        laps = sanitizedLaps,
                        name = sanitizedRunName,
                    )
                }

                StopwatchItem(
                    id = UUID.randomUUID().toString(),
                    name = sanitizedName,
                    pastelColorHex = safeColor,
                    isRunning = false,
                    runs = sanitizedRuns,
                )
            }

            payload.copy(
                appVersion = sanitizedAppVersion,
                stopwatches = sanitizedWatches,
            )
        }
    }

    /**
     * Reads and parses a backup payload from an Android content or file [Uri] with a 5 MB maximum size limit.
     */
    fun parseBackupFromUri(context: Context, uri: Uri): Result<StopwatchBackupPayload> {
        return runCatching {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open URI: $uri")
            val jsonContent = inputStream.use { stream ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var totalBytes = 0
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    totalBytes += read
                    if (totalBytes > MAX_BACKUP_SIZE_BYTES) {
                        throw IllegalArgumentException("File exceeds maximum allowed backup size")
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
            parseBackupJson(jsonContent).getOrThrow()
        }
    }

    private fun String.sanitizeText(): String {
        return this.filter { char ->
            !char.isISOControl() && char != '\u0000' && char != '\uFEFF'
        }.trim()
    }
}
