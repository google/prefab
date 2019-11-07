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

import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndToEndTest {
    private val outputDirectory: Path =
        Files.createTempDirectory("output").apply {
        toFile().apply { deleteOnExit() }
    }

    @Test
    fun `unknown dependencies are an error`() {
        val packagePath =
            Paths.get(this.javaClass.getResource("packages/foo").toURI())

        val ex = assertFailsWith(FatalApplicationError::class) {
            Cli().main(
                listOf(
                    "--platform", "android",
                    "--abi", "arm64-v8a",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--build-system", "ndk-build",
                    "--output", outputDirectory.toString(),
                    packagePath.toString()
                )
            )
        }

        assertEquals("Error: foo depends on unknown dependency qux", ex.message)
    }
}
