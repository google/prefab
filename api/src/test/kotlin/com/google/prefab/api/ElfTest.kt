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

import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElfTest {
    @Test
    fun `can find single shared library`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        val expectedLib: Path = libDir.resolve("$name.so").apply {
            toFile().createNewFile()
        }

        val foundLib = findElfLibrary(libDir, name)
        assertEquals(foundLib, expectedLib)
    }

    @Test
    fun `can find single static library`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        val expectedLib: Path = libDir.resolve("$name.a").apply {
            toFile().createNewFile()
        }

        val foundLib = findElfLibrary(libDir, name)
        assertEquals(foundLib, expectedLib)
    }

    @Test
    fun `can find library with when other files are present`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        val expectedLib: Path = libDir.resolve("$name.so").apply {
            toFile().createNewFile()
        }

        libDir.resolve("some_other_file.json").toFile().createNewFile()

        val foundLib = findElfLibrary(libDir, name)
        assertEquals(foundLib, expectedLib)
    }

    @Test
    fun `does not find library with wrong extension`() {
        val name = "foo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        libDir.resolve("$name.dll").toFile().createNewFile()

        assertFailsWith(RuntimeException::class) {
            findElfLibrary(libDir, name)
        }
    }

    @Test
    fun `does not find library with wrong name`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        libDir.resolve("libbar.so").toFile().createNewFile()

        assertFailsWith(RuntimeException::class) {
            findElfLibrary(libDir, name)
        }
    }

    @Test
    fun `throws if no libraries are found`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        assertFailsWith(RuntimeException::class) {
            findElfLibrary(libDir, name)
        }
    }

    @Test
    fun `throws if multiple libraries are found`() {
        val name = "libfoo"

        val libDir: Path = Files.createTempDirectory("libdir").apply {
            toFile().deleteOnExit()
        }

        libDir.resolve("$name.a").toFile().createNewFile()
        libDir.resolve("$name.so").toFile().createNewFile()

        assertFailsWith(RuntimeException::class) {
            findElfLibrary(libDir, name)
        }
    }
}