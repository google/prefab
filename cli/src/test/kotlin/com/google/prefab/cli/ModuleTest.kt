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
import com.google.prefab.api.LibraryReference
import com.google.prefab.api.Module
import com.google.prefab.api.NoMatchingLibraryException
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import com.google.prefab.api.SchemaVersion
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class ModuleTest(override val schemaVersion: SchemaVersion) : PerSchemaTest {
    private val android: PlatformDataInterface =
        Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)

    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

    @Test
    fun `can load basic module`() {
        val pkg = mockk<Package>()
        every { pkg.name } returns "foo"

        val modulePath = packagePath("foo").resolve("modules/bar")

        val module = Module(modulePath, pkg, schemaVersion)
        assertEquals(modulePath.fileName.toString(), module.name)
        assertEquals("//foo/bar", module.canonicalName)
        assertEquals(modulePath, module.path)
        assertEquals("libbar", module.libraryNameForPlatform(android))
        assertEquals(
            listOf(LibraryReference.Literal("-landroid")),
            module.linkLibsForPlatform(android)
        )

        assertEquals(5, module.libraries.size)
    }

    @Test
    fun `can load static library`() {
        val pkg = mockk<Package>()
        every { pkg.name } returns "qux"

        val modulePath = packagePath("qux").resolve("modules/libqux")

        val module = Module(modulePath, pkg, schemaVersion)
        assertEquals(modulePath.fileName.toString(), module.name)
        assertEquals("//qux/libqux", module.canonicalName)
        assertEquals(modulePath, module.path)
        assertEquals("libqux", module.libraryNameForPlatform(android))
        assertEquals(
            listOf(LibraryReference.External("foo", "bar")),
            module.linkLibsForPlatform(android)
        )

        assertEquals(5, module.libraries.size)
    }

    @Test
    fun `best available match is found`() {
        val pkg = mockk<Package>()
        every { pkg.name } returns "find_best_match"

        val byApi = Module(
            packagePath("find_best_match").resolve("modules/byapi"),
            pkg,
            schemaVersion
        )

        val lollipop = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val marshmallow =
            Android(Android.Abi.Arm64, 23, Android.Stl.CxxShared, 21)
        val nougat = Android(Android.Abi.Arm64, 24, Android.Stl.CxxShared, 21)
        val oreo = Android(Android.Abi.Arm64, 26, Android.Stl.CxxShared, 21)
        val pie = Android(Android.Abi.Arm64, 28, Android.Stl.CxxShared, 21)

        val ex = assertFailsWith(NoMatchingLibraryException::class) {
            byApi.getLibraryFor(lollipop)
        }

        assertEquals(
            """
            No compatible library found for //find_best_match/byapi. Rejected the following libraries:
            android.arm64-v8a-23: User has minSdkVersion 21 but library was built for 23
            android.arm64-v8a-24: User has minSdkVersion 21 but library was built for 24
            android.arm64-v8a-28: User has minSdkVersion 21 but library was built for 28
            """.trimIndent(), ex.message
        )
        assertEquals(
            23, (byApi.getLibraryFor(marshmallow).platform as Android).api
        )
        assertEquals(24, (byApi.getLibraryFor(nougat).platform as Android).api)
        assertEquals(24, (byApi.getLibraryFor(oreo).platform as Android).api)
        assertEquals(28, (byApi.getLibraryFor(pie).platform as Android).api)

        val byNdk = Module(
            packagePath("find_best_match").resolve(
                "modules/byndk"
            ), pkg, schemaVersion
        )

        val r18 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18)
        val r19 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 19)
        val r20 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 20)
        val r21 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val r22 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 22)

        assertEquals(
            19, (byNdk.getLibraryFor(r18).platform as Android).ndkMajorVersion
        )
        assertEquals(
            19, (byNdk.getLibraryFor(r19).platform as Android).ndkMajorVersion
        )
        assertEquals(
            20, (byNdk.getLibraryFor(r20).platform as Android).ndkMajorVersion
        )
        assertEquals(
            21, (byNdk.getLibraryFor(r21).platform as Android).ndkMajorVersion
        )
        assertEquals(
            21, (byNdk.getLibraryFor(r22).platform as Android).ndkMajorVersion
        )
    }
}
