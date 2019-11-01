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

import com.google.prefab.api.Android
import com.google.prefab.api.Package
import com.google.prefab.cmake.CMakePlugin
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CMakePluginTest {
    private val staleOutputDir: Path =
        Files.createTempDirectory("stale").apply {
            toFile().apply { deleteOnExit() }
        }

    private val staleFile: Path = staleOutputDir.resolve("bogus").apply {
        toFile().createNewFile()
    }

    private val outputDirectory: Path =
        Files.createTempDirectory("output").apply {
            toFile().apply { deleteOnExit() }
        }

    @Test
    fun `multi-target generations are rejected`() {
        val generator = CMakePlugin(staleOutputDir.toFile(), emptyList())
        val requirements =
            Android.Abi.values().map { Android(it, 21) }
        assertTrue(requirements.size > 1)
        assertThrows<UnsupportedOperationException> {
            generator.generate(requirements)
        }
    }

    @Test
    fun `stale files are removed from extant output directory`() {
        assertTrue(staleFile.toFile().exists())
        CMakePlugin(staleOutputDir.toFile(), emptyList()).generate(
            listOf(Android(Android.Abi.Arm64, 21))
        )
        assertFalse(staleFile.toFile().exists())
    }

    @Test
    fun `basic project generates correctly`() {
        val fooPath =
            Paths.get(this.javaClass.getResource("packages/foo").toURI())
        val foo = Package(fooPath)

        val quxPath =
            Paths.get(this.javaClass.getResource("packages/qux").toURI())
        val qux = Package(quxPath)

        CMakePlugin(outputDirectory.toFile(), listOf(foo, qux)).generate(
            listOf(Android(Android.Abi.Arm64, 19))
        )

        val fooConfigFile =
            outputDirectory.resolve("${foo.name}-config.cmake").toFile()
        assertTrue(fooConfigFile.exists())

        val quxConfigFile =
            outputDirectory.resolve("${qux.name}-config.cmake").toFile()
        assertTrue(quxConfigFile.exists())

        val barDir = fooPath.resolve("modules/bar")
        val bazDir = fooPath.resolve("modules/baz")
        assertEquals(
            """
            find_package(quux REQUIRED)

            find_package(qux REQUIRED)

            add_library(foo::bar SHARED IMPORTED)
            set_target_properties(foo::bar PROPERTIES
                IMPORTED_LOCATION "$barDir/libs/android.arm64-v8a/libbar.so"
                INTERFACE_INCLUDE_DIRECTORIES "$barDir/include"
                INTERFACE_LINK_LIBRARIES "-landroid"
            )

            add_library(foo::baz SHARED IMPORTED)
            set_target_properties(foo::baz PROPERTIES
                IMPORTED_LOCATION "$bazDir/libs/android.arm64-v8a/libbaz.so"
                INTERFACE_INCLUDE_DIRECTORIES "$bazDir/include"
                INTERFACE_LINK_LIBRARIES "-llog;foo::bar;qux::libqux"
            )


            """.trimIndent(), fooConfigFile.readText()
        )

        val quxDir = quxPath.resolve("modules/libqux")
        assertEquals(
            """
            find_package(foo REQUIRED)

            add_library(qux::libqux SHARED IMPORTED)
            set_target_properties(qux::libqux PROPERTIES
                IMPORTED_LOCATION "$quxDir/libs/android.arm64-v8a/libqux.a"
                INTERFACE_INCLUDE_DIRECTORIES "$quxDir/include"
                INTERFACE_LINK_LIBRARIES "foo::bar"
            )


            """.trimIndent(), quxConfigFile.readText()
        )
    }

    @Test
    fun `header only module works`() {
        val packagePath =
            Paths.get(this.javaClass.getResource("packages/header_only").toURI())
        val pkg = Package(packagePath)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21))
        )

        val name = pkg.name
        val configFile = outputDirectory.resolve("$name-config.cmake").toFile()
        assertTrue(configFile.exists())

        val fooDir = packagePath.resolve("modules/foo")
        val barDir = packagePath.resolve("modules/bar")
        assertEquals(
            """
            add_library(header_only::bar SHARED IMPORTED)
            set_target_properties(header_only::bar PROPERTIES
                IMPORTED_LOCATION "$barDir/libs/android.arm64-v8a/libbar.so"
                INTERFACE_INCLUDE_DIRECTORIES "$barDir/include"
                INTERFACE_LINK_LIBRARIES "header_only::foo"
            )

            add_library(header_only::foo INTERFACE)
            set_target_properties(header_only::foo PROPERTIES
                INTERFACE_INCLUDE_DIRECTORIES "$fooDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )


            """.trimIndent(), configFile.readText()
        )
    }
}
