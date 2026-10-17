package org.ear.watches.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import org.ear.watches.R
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.StopwatchItem
import java.io.File

/**
 * Helper utility for exporting formatted race results via the Android Share Sheet.
 * Utilizes [Intent.ACTION_SEND] to share text-based reports or CSV spreadsheet files with external applications.
 */
object ShareHelper {

    fun shareRaceResults(
        context: Context,
        resultsText: String,
        subject: String? = null,
        chooserTitle: String? = null
    ) {
        val shareSubject = subject ?: context.getString(R.string.share_subject)
        val title = chooserTitle ?: context.getString(R.string.share_chooser_title)
        val errorToastText = context.getString(R.string.share_error_toast)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, resultsText)
        }

        try {
            val chooser = Intent.createChooser(shareIntent, title)
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, errorToastText, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSingleStopwatchResults(
        context: Context,
        watch: StopwatchItem,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val subsecondSeparator = context.getString(R.string.subsecond_separator)
        val resultsText = RaceResultsFormatter.formatSingleStopwatchResults(
            context = context,
            watch = watch,
            precision = precision,
            nowMillis = nowMillis,
            subsecondSeparator = subsecondSeparator
        )
        shareRaceResults(context, resultsText)
    }

    fun shareRaceResultsCsv(
        context: Context,
        stopwatches: List<StopwatchItem>,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis(),
        subject: String? = null,
        chooserTitle: String? = null
    ) {
        val shareSubject = subject ?: context.getString(R.string.share_subject)
        val title = chooserTitle ?: context.getString(R.string.share_chooser_title)
        val errorToastText = context.getString(R.string.share_error_toast)

        val csvText = RaceResultsFormatter.formatAsCsv(
            stopwatches = stopwatches,
            precision = precision,
            nowMillis = nowMillis
        )

        try {
            val cacheFile = File(context.cacheDir, "race_results.csv")
            cacheFile.writeText(csvText)

            val contentUri = FileProvider.getUriForFile(
                context,
                "org.ear.watches.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title)
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, errorToastText, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSingleStopwatchResultsCsv(
        context: Context,
        watch: StopwatchItem,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        shareRaceResultsCsv(
            context = context,
            stopwatches = listOf(watch),
            precision = precision,
            nowMillis = nowMillis
        )
    }
}

fun shareRaceResults(
    context: Context,
    resultsText: String,
    subject: String? = null,
    chooserTitle: String? = null
) {
    ShareHelper.shareRaceResults(context, resultsText, subject, chooserTitle)
}
