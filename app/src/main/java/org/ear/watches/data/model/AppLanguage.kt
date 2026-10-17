package org.ear.watches.data.model

import androidx.annotation.StringRes
import org.ear.watches.R
import kotlinx.serialization.Serializable

/**
 * Defines the supported languages for the application.
 * Contains the BCP-47 language tag and the corresponding localized string resource for display.
 */
@Serializable
enum class AppLanguage(
    val languageTag: String,
    @StringRes val labelResId: Int
) {
    SYSTEM("", R.string.language_system_default),
    ENGLISH("en", R.string.language_english),
    GERMAN("de", R.string.language_german),
    FRENCH("fr", R.string.language_french)
}
