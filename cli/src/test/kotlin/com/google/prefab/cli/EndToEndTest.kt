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

package com.google.prefab.cli

import com.google.prefab.api.SchemaVersion
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class EndToEndTest(override val schemaVersion: SchemaVersion) : PerSchemaTest {
    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

    private val outputDirectory: Path =
        Files.createTempDirectory("output").apply {
        toFile().apply { deleteOnExit() }
    }

    @Test
    fun `unknown dependencies are an error`() {
        val path = packagePath("foo")

        val ex = assertFailsWith(FatalApplicationError::class) {
            Cli().main(
                listOf(
                    "--platform", "android",
                    "--abi", "arm64-v8a",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--ndk-version", "21",
                    "--build-system", "ndk-build",
                    "--output", outputDirectory.toString(),
                    path.toString()
                )
            )
        }

        assertEquals("Error: foo depends on unknown dependency qux", ex.message)
    }
}
