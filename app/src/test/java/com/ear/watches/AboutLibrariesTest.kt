package com.ear.watches

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AboutLibrariesTest {

    @Test
    fun testAboutLibrariesJsonGeneratedAndValid() {
        val jsonFile = File("build/generated/aboutLibraries/aboutlibraries.json")
        assertTrue(jsonFile.exists(), "aboutlibraries.json should be generated at build/generated/aboutLibraries/aboutlibraries.json")

        val jsonString = jsonFile.readText()
        assertTrue(jsonString.isNotBlank(), "aboutlibraries.json should not be empty")

        val root = Json.parseToJsonElement(jsonString).jsonObject
        assertTrue(root.containsKey("libraries"), "JSON should contain 'libraries' key")

        val librariesArray = root["libraries"]?.jsonArray
        assertTrue(librariesArray != null && librariesArray.isNotEmpty(), "Libraries list in aboutlibraries.json should not be empty")
    }
}
