package org.ear.watches

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class AboutLibrariesTest {

    @Test
    fun testAboutLibrariesJsonGeneratedAndValid() {
        val path = "build/generated/aboutLibraries/debug/res/raw/aboutlibraries.json"
        val jsonFile = File(path).let { if (it.exists()) it else File("app/$path") }
        assertTrue(jsonFile.exists(), "aboutlibraries.json should be generated at $path")

        val jsonString = jsonFile.readText()
        assertTrue(jsonString.isNotBlank(), "aboutlibraries.json should not be empty")

        val root = Json.parseToJsonElement(jsonString).jsonObject
        assertTrue(root.containsKey("libraries"), "JSON should contain 'libraries' key")

        val librariesArray = root["libraries"]?.jsonArray
        assertTrue(!librariesArray.isNullOrEmpty(), "Libraries list in aboutlibraries.json should not be empty")
    }
}
