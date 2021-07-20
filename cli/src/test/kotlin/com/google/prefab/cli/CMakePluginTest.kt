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
import com.google.prefab.api.SchemaVersion
import com.google.prefab.cmake.CMakePlugin
import com.google.prefab.cmake.sanitize
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class CMakePluginTest(override val schemaVersion: SchemaVersion) :
    PerSchemaTest {

    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

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

    private fun cmakeConfigName(packageName: String): String =
        "${packageName}Config"

    private fun cmakeConfigFile(packageName: String): String =
        "${cmakeConfigName(packageName)}.cmake"

    private fun cmakeVersionFile(packageName: String): String =
        "${cmakeConfigName(packageName)}Version.cmake"

    @Test
    fun `multi-target generations are rejected`() {
        val generator = CMakePlugin(staleOutputDir.toFile(), emptyList())
        val requirements = Android.Abi.values()
            .map { Android(it, 21, Android.Stl.CxxShared, 21) }
        assertTrue(requirements.size > 1)
        assertThrows<UnsupportedOperationException> {
            generator.generate(requirements)
        }
    }

    @Test
    fun `stale files are removed from extant output directory`() {
        assertTrue(staleFile.toFile().exists())
        CMakePlugin(staleOutputDir.toFile(), emptyList()).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21))
        )
        assertFalse(staleFile.toFile().exists())
    }

    @Test
    fun `basic project generates correctly`() {
        val fooPath = packagePath("foo")
        val foo = Package(fooPath)

        val quxPath = packagePath("qux")
        val qux = Package(quxPath)

        CMakePlugin(outputDirectory.toFile(), listOf(foo, qux)).generate(
            listOf(Android(Android.Abi.Arm64, 19, Android.Stl.CxxShared, 21))
        )

        val archDir = outputDirectory.resolve("lib/aarch64-linux-android/cmake")
        val fooConfigFile =
            archDir.resolve("${foo.name}/${cmakeConfigFile(foo.name)}").toFile()
        val fooVersionFile = archDir.resolve(
            "${foo.name}/${cmakeVersionFile(foo.name)}"
        ).toFile()
        assertTrue(fooConfigFile.exists())
        assertTrue(fooVersionFile.exists())

        val quxConfigFile =
            archDir.resolve("${qux.name}/${cmakeConfigFile(qux.name)}").toFile()
        val quxVersionFile = archDir.resolve(
            "${qux.name}/${cmakeVersionFile(qux.name)}"
        ).toFile()
        assertTrue(quxConfigFile.exists())
        assertTrue(quxVersionFile.exists())

        val barDir = fooPath.resolve("modules/bar").sanitize()
        val bazDir = fooPath.resolve("modules/baz").sanitize()
        assertEquals(
            """
            find_package(quux REQUIRED CONFIG)

            find_package(qux REQUIRED CONFIG)

            if(NOT TARGET foo::bar)
            add_library(foo::bar SHARED IMPORTED)
            set_target_properties(foo::bar PROPERTIES
                IMPORTED_LOCATION "$barDir/libs/android.arm64-v8a/libbar.so"
                INTERFACE_INCLUDE_DIRECTORIES "$barDir/include"
                INTERFACE_LINK_LIBRARIES "-landroid"
            )
            endif()

            if(NOT TARGET foo::baz)
            add_library(foo::baz SHARED IMPORTED)
            set_target_properties(foo::baz PROPERTIES
                IMPORTED_LOCATION "$bazDir/libs/android.arm64-v8a/libbaz.so"
                INTERFACE_INCLUDE_DIRECTORIES "$bazDir/include"
                INTERFACE_LINK_LIBRARIES "-llog;foo::bar;qux::libqux"
            )
            endif()


            """.trimIndent(), fooConfigFile.readText()
        )

        assertEquals(
            """
            set(PACKAGE_VERSION 1)
            if("${'$'}{PACKAGE_VERSION}" VERSION_LESS "${'$'}{PACKAGE_FIND_VERSION}")
                set(PACKAGE_VERSION_COMPATIBLE FALSE)
            else()
                set(PACKAGE_VERSION_COMPATIBLE TRUE)
                if("${'$'}{PACKAGE_VERSION}" VERSION_EQUAL "${'$'}{PACKAGE_FIND_VERSION}")
                    set(PACKAGE_VERSION_EXACT TRUE)
                endif()
            endif()
            """.trimIndent(), fooVersionFile.readText()
        )

        val quxDir = quxPath.resolve("modules/libqux").sanitize()
        assertEquals(
            """
            find_package(foo REQUIRED CONFIG)

            if(NOT TARGET qux::libqux)
            add_library(qux::libqux STATIC IMPORTED)
            set_target_properties(qux::libqux PROPERTIES
                IMPORTED_LOCATION "$quxDir/libs/android.arm64-v8a/libqux.a"
                INTERFACE_INCLUDE_DIRECTORIES "$quxDir/include"
                INTERFACE_LINK_LIBRARIES "foo::bar"
            )
            endif()


            """.trimIndent(), quxConfigFile.readText()
        )

        assertEquals(
            """
            set(PACKAGE_VERSION 1.2.1.4)
            if("${'$'}{PACKAGE_VERSION}" VERSION_LESS "${'$'}{PACKAGE_FIND_VERSION}")
                set(PACKAGE_VERSION_COMPATIBLE FALSE)
            else()
                set(PACKAGE_VERSION_COMPATIBLE TRUE)
                if("${'$'}{PACKAGE_VERSION}" VERSION_EQUAL "${'$'}{PACKAGE_FIND_VERSION}")
                    set(PACKAGE_VERSION_EXACT TRUE)
                endif()
            endif()
            """.trimIndent(), quxVersionFile.readText()
        )
    }

    @Test
    fun `header only module works`() {
        val packagePath = packagePath("header_only")
        val pkg = Package(packagePath)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21))
        )

        val name = pkg.name
        val archDir = outputDirectory.resolve("lib/aarch64-linux-android/cmake")
        val configFile =
            archDir.resolve("$name/${cmakeConfigFile(name)}").toFile()
        val versionFile =
            archDir.resolve("$name/${cmakeVersionFile(name)}").toFile()
        assertTrue(configFile.exists())
        assertTrue(versionFile.exists())

        val fooDir = packagePath.resolve("modules/foo").sanitize()
        val barDir = packagePath.resolve("modules/bar").sanitize()
        assertEquals(
            """
            if(NOT TARGET header_only::bar)
            add_library(header_only::bar SHARED IMPORTED)
            set_target_properties(header_only::bar PROPERTIES
                IMPORTED_LOCATION "$barDir/libs/android.arm64-v8a/libbar.so"
                INTERFACE_INCLUDE_DIRECTORIES "$barDir/include"
                INTERFACE_LINK_LIBRARIES "header_only::foo"
            )
            endif()

            if(NOT TARGET header_only::foo)
            add_library(header_only::foo INTERFACE IMPORTED)
            set_target_properties(header_only::foo PROPERTIES
                INTERFACE_INCLUDE_DIRECTORIES "$fooDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )

        assertEquals(
            """
            set(PACKAGE_VERSION 2.2)
            if("${'$'}{PACKAGE_VERSION}" VERSION_LESS "${'$'}{PACKAGE_FIND_VERSION}")
                set(PACKAGE_VERSION_COMPATIBLE FALSE)
            else()
                set(PACKAGE_VERSION_COMPATIBLE TRUE)
                if("${'$'}{PACKAGE_VERSION}" VERSION_EQUAL "${'$'}{PACKAGE_FIND_VERSION}")
                    set(PACKAGE_VERSION_EXACT TRUE)
                endif()
            endif()
            """.trimIndent(), versionFile.readText()
        )
    }

    @Test
    fun `per-platform includes work`() {
        val path = packagePath("per_platform_includes")
        val pkg = Package(path)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 19))
        )

        val name = pkg.name
        var archDir = outputDirectory.resolve("lib/aarch64-linux-android/cmake")
        var configFile =
            archDir.resolve("$name/${cmakeConfigFile(name)}").toFile()
        val versionFile =
            archDir.resolve("$name/${cmakeVersionFile(name)}").toFile()
        assertTrue(configFile.exists())
        // No version is provided for this package, so we shouldn't provide a
        // version file.
        assertFalse(versionFile.exists())

        val modDir = path.resolve("modules/perplatform").sanitize()
        assertEquals(
            """
            if(NOT TARGET per_platform_includes::perplatform)
            add_library(per_platform_includes::perplatform SHARED IMPORTED)
            set_target_properties(per_platform_includes::perplatform PROPERTIES
                IMPORTED_LOCATION "$modDir/libs/android.arm64-v8a/libperplatform.so"
                INTERFACE_INCLUDE_DIRECTORIES "$modDir/libs/android.arm64-v8a/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )

        // Only some of the platforms in this module have their own headers.
        // Verify that the module level headers are used for platforms that
        // don't.
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.X86_64, 21, Android.Stl.CxxShared, 19))
        )

        archDir = outputDirectory.resolve("lib/x86_64-linux-android/cmake")
        configFile = archDir.resolve("$name/${cmakeConfigFile(name)}").toFile()

        assertEquals(
            """
            if(NOT TARGET per_platform_includes::perplatform)
            add_library(per_platform_includes::perplatform SHARED IMPORTED)
            set_target_properties(per_platform_includes::perplatform PROPERTIES
                IMPORTED_LOCATION "$modDir/libs/android.x86_64/libperplatform.so"
                INTERFACE_INCLUDE_DIRECTORIES "$modDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )
    }

    @Test
    fun `old NDKs use non-arch specific layout`() {
        val packagePath = packagePath("header_only")
        val pkg = Package(packagePath)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18))
        )

        val name = pkg.name
        val configFile = outputDirectory.resolve(cmakeConfigFile(name)).toFile()
        val versionFile =
            outputDirectory.resolve(cmakeVersionFile(name)).toFile()
        assertTrue(configFile.exists())
        assertTrue(versionFile.exists())

        val fooDir = packagePath.resolve("modules/foo").sanitize()
        val barDir = packagePath.resolve("modules/bar").sanitize()
        assertEquals(
            """
            if(NOT TARGET header_only::bar)
            add_library(header_only::bar SHARED IMPORTED)
            set_target_properties(header_only::bar PROPERTIES
                IMPORTED_LOCATION "$barDir/libs/android.arm64-v8a/libbar.so"
                INTERFACE_INCLUDE_DIRECTORIES "$barDir/include"
                INTERFACE_LINK_LIBRARIES "header_only::foo"
            )
            endif()

            if(NOT TARGET header_only::foo)
            add_library(header_only::foo INTERFACE IMPORTED)
            set_target_properties(header_only::foo PROPERTIES
                INTERFACE_INCLUDE_DIRECTORIES "$fooDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )

        assertEquals(
            """
            set(PACKAGE_VERSION 2.2)
            if("${'$'}{PACKAGE_VERSION}" VERSION_LESS "${'$'}{PACKAGE_FIND_VERSION}")
                set(PACKAGE_VERSION_COMPATIBLE FALSE)
            else()
                set(PACKAGE_VERSION_COMPATIBLE TRUE)
                if("${'$'}{PACKAGE_VERSION}" VERSION_EQUAL "${'$'}{PACKAGE_FIND_VERSION}")
                    set(PACKAGE_VERSION_EXACT TRUE)
                endif()
            endif()
            """.trimIndent(), versionFile.readText()
        )
    }

    @Test
    fun `mixed static and shared libraries are both exposed when compatible`() {
        val packagePath = packagePath("static_and_shared")
        val pkg = Package(packagePath)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18))
        )

        val configFile =
            outputDirectory.resolve(cmakeConfigFile(pkg.name)).toFile()
        assertTrue(configFile.exists())

        val fooDir = packagePath.resolve("modules/foo").sanitize()
        val fooStaticDir = packagePath.resolve("modules/foo_static").sanitize()
        assertEquals(
            """
            if(NOT TARGET static_and_shared::foo)
            add_library(static_and_shared::foo SHARED IMPORTED)
            set_target_properties(static_and_shared::foo PROPERTIES
                IMPORTED_LOCATION "$fooDir/libs/android.shared/libfoo.so"
                INTERFACE_INCLUDE_DIRECTORIES "$fooDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()

            if(NOT TARGET static_and_shared::foo_static)
            add_library(static_and_shared::foo_static STATIC IMPORTED)
            set_target_properties(static_and_shared::foo_static PROPERTIES
                IMPORTED_LOCATION "$fooStaticDir/libs/android.static/libfoo.a"
                INTERFACE_INCLUDE_DIRECTORIES "$fooStaticDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )
    }

    @Test
    fun `incompatible libraries are skipped for static STL`() {
        val packagePath = packagePath("static_and_shared")
        val pkg = Package(packagePath)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxStatic, 18))
        )

        val configFile =
            outputDirectory.resolve(cmakeConfigFile(pkg.name)).toFile()
        assertTrue(configFile.exists())

        val fooStaticDir = packagePath.resolve("modules/foo_static").sanitize()
        assertEquals(
            """
            if(NOT TARGET static_and_shared::foo_static)
            add_library(static_and_shared::foo_static STATIC IMPORTED)
            set_target_properties(static_and_shared::foo_static PROPERTIES
                IMPORTED_LOCATION "$fooStaticDir/libs/android.static/libfoo.a"
                INTERFACE_INCLUDE_DIRECTORIES "$fooStaticDir/include"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )
    }

    @Test
    fun `empty include directories are skipped`() {
        // Regression test for https://issuetracker.google.com/178594838.
        val path = packagePath("no_headers")
        val pkg = Package(path)
        CMakePlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18))
        )

        val configFile =
            outputDirectory.resolve(cmakeConfigFile(pkg.name)).toFile()
        assertTrue(configFile.exists())

        val moduleDir = path.resolve("modules/runtime").sanitize()
        assertEquals(
            """
            if(NOT TARGET no_headers::runtime)
            add_library(no_headers::runtime SHARED IMPORTED)
            set_target_properties(no_headers::runtime PROPERTIES
                IMPORTED_LOCATION "$moduleDir/libs/android.arm64-v8a/libruntime.so"
                INTERFACE_LINK_LIBRARIES ""
            )
            endif()


            """.trimIndent(), configFile.readText()
        )
    }
}
