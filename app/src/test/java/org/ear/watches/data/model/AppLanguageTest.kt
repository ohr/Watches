package org.ear.watches.data.model

import org.ear.watches.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AppLanguageTest {

    @Test
    fun testAppLanguageEntriesAndProperties() {
        val entries = AppLanguage.entries
        assertEquals(4, entries.size)

        val system = AppLanguage.valueOf("SYSTEM")
        assertEquals("", system.languageTag)
        assertEquals(R.string.language_system_default, system.labelResId)

        val english = AppLanguage.valueOf("ENGLISH")
        assertEquals("en", english.languageTag)
        assertEquals(R.string.language_english, english.labelResId)

        val german = AppLanguage.valueOf("GERMAN")
        assertEquals("de", german.languageTag)
        assertEquals(R.string.language_german, german.labelResId)

        val french = AppLanguage.valueOf("FRENCH")
        assertEquals("fr", french.languageTag)
        assertEquals(R.string.language_french, french.labelResId)
    }

    @Test
    fun testAllEntriesHaveNonNullLabelsAndTags() {
        AppLanguage.entries.forEach { language ->
            assertNotNull(language.languageTag)
            assert(language.labelResId != 0)
        }
    }
}
