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
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleTest {
    private val android: PlatformDataInterface = Android(Android.Abi.Arm64, 21)

    @Test
    fun `can load basic module`() {
        val pkg = mockk<Package>()
        every { pkg.name } returns "foo"

        val modulePath = Paths.get(
            this.javaClass.getResource("packages/foo/modules/bar").toURI()
        )

        val module = Module(modulePath, pkg)
        assertEquals(modulePath.fileName.toString(), module.name)
        assertEquals("//foo/bar", module.canonicalName)
        assertEquals(modulePath, module.path)
        assertEquals("libbar", module.libraryNameForPlatform(android))
        assertEquals(
            listOf(LibraryReference.Literal("-landroid")),
            module.linkLibsForPlatform(android)
        )

        assertEquals(4, module.libraries.size)
    }

    @Test
    fun `can load static library`() {
        val pkg = mockk<Package>()
        every { pkg.name } returns "qux"

        val modulePath = Paths.get(
            this.javaClass.getResource("packages/qux/modules/libqux").toURI()
        )

        val module = Module(modulePath, pkg)
        assertEquals(modulePath.fileName.toString(), module.name)
        assertEquals("//qux/libqux", module.canonicalName)
        assertEquals(modulePath, module.path)
        assertEquals("libqux", module.libraryNameForPlatform(android))
        assertEquals(
            listOf(LibraryReference.External("foo", "bar")),
            module.linkLibsForPlatform(android)
        )

        assertEquals(4, module.libraries.size)
    }
}
