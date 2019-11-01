/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.prefab.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.parse
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleMetadataTest {
    @Test
    fun `fails if object has unknown keys`() {
        assertFailsWith(JsonDecodingException::class) {
            Json.parse<ModuleMetadataV1>(
                """
                {
                    "export_libraries": [],
                    "bad": "value"
                }
                """.trimIndent()
            )
        }
    }

    @Test
    fun `minimum valid metadata loads correctly`() {
        val moduleMetadata = Json.parse<ModuleMetadataV1>(
            """
            {
                "export_libraries": []
            }
            """.trimIndent()
        )
        assertEquals(emptyList(), moduleMetadata.exportLibraries)
        assertEquals(null, moduleMetadata.libraryName)
        assertEquals(null, moduleMetadata.android.exportLibraries)
        assertEquals(null, moduleMetadata.android.libraryName)
    }

    @Test
    fun `metadata with no platform specific data loads correctly`() {
        val moduleMetadata = Json.parse<ModuleMetadataV1>(
            """
            {
                "export_libraries": ["-lm"],
                "library_name": "libmylibrary"
            }
            """.trimIndent()
        )
        assertEquals(listOf("-lm"), moduleMetadata.exportLibraries)
        assertEquals("libmylibrary", moduleMetadata.libraryName)
        assertEquals(null, moduleMetadata.android.exportLibraries)
        assertEquals(null, moduleMetadata.android.libraryName)
    }

    @Test
    fun `metadata with partial platform specific data loads correctly`() {
        val moduleMetadata = Json.parse<ModuleMetadataV1>(
            """
            {
                "export_libraries": [],
                "android": {
                    "export_libraries": ["-landroid"]
                }
            }
            """.trimIndent()
        )
        assertEquals(emptyList(), moduleMetadata.exportLibraries)
        assertEquals(null, moduleMetadata.libraryName)
        assertEquals(
            listOf("-landroid"),
            moduleMetadata.android.exportLibraries
        )
        assertEquals(null, moduleMetadata.android.libraryName)
    }

    @Test
    fun `metadata will all values set loads correctly`() {
        val moduleMetadata = Json.parse<ModuleMetadataV1>(
            """
            {
                "export_libraries": [":bar"],
                "library_name": "libfoo",
                "android": {
                    "export_libraries": ["-llog"],
                    "library_name": "libfoo_android"
                }
            }
            """.trimIndent()
        )
        assertEquals(listOf(":bar"), moduleMetadata.exportLibraries)
        assertEquals("libfoo", moduleMetadata.libraryName)
        assertEquals(listOf("-llog"), moduleMetadata.android.exportLibraries)
        assertEquals("libfoo_android", moduleMetadata.android.libraryName)
    }
}
