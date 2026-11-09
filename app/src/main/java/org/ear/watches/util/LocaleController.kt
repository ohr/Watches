package org.ear.watches.util

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.ear.watches.data.model.AppLanguage

/**
 * Reads and applies the app language. Abstracted so the ViewModel stays testable on the JVM.
 */
interface LocaleController {
    /** The language currently applied to the app ([AppLanguage.SYSTEM] if none is set). */
    fun current(): AppLanguage

    /** Applies [language] app-wide (recreates visible activities). */
    fun apply(language: AppLanguage)

    /**
     * Returns the effective language given the [stored] setting.
     *
     * The platform's per-app language is the source of truth, since on Android 13+ users can also
     * change it in the system settings. The only exception is migrating a language stored by
     * older app versions on Android < 13, where no platform value existed yet.
     */
    fun resolve(stored: AppLanguage): AppLanguage {
        val current = current()
        if (current == AppLanguage.SYSTEM && stored != AppLanguage.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            apply(stored)
            return stored
        }
        return current
    }
}

/**
 * [LocaleController] backed by [AppCompatDelegate.setApplicationLocales]: uses the platform
 * per-app language API on Android 13+ and AppCompat's own storage on older versions
 * (see `AppLocalesMetadataHolderService` in the manifest).
 */
class AppCompatLocaleController : LocaleController {

    override fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val language = locales[0]?.language ?: return AppLanguage.SYSTEM
        return AppLanguage.entries.firstOrNull { it.languageTag.isNotEmpty() && it.languageTag == language }
            ?: AppLanguage.SYSTEM
    }

    override fun apply(language: AppLanguage) {
        if (current() == language) return
        val locales = if (language.languageTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
