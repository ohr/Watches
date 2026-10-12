package com.ear.watches.data.model

import androidx.annotation.StringRes
import com.ear.watches.R

enum class AppLanguage(
    val languageTag: String,
    @StringRes val labelResId: Int
) {
    SYSTEM("", R.string.language_system_default),
    ENGLISH("en", R.string.language_english),
    GERMAN("de", R.string.language_german)
}
