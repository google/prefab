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
import com.google.prefab.api.MissingArtifactIDException
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import com.google.prefab.api.UnsupportedPlatformException
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PackageTest {
    private val android: PlatformDataInterface =
        Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared)

    @Test
    fun `can load basic package`() {
        val packagePath =
            Paths.get(this.javaClass.getResource("packages/foo").toURI())
        val pkg = Package(packagePath)
        assertEquals(packagePath, pkg.path)
        assertEquals("foo", pkg.name)
        assertEquals(listOf("qux", "quux"), pkg.dependencies)

        assertEquals(2, pkg.modules.size)

        val (bar, baz) = pkg.modules.sortedBy { it.name }

        assertEquals("bar", bar.name)
        assertEquals(packagePath.resolve("modules/bar"), bar.path)
        assertEquals("libbar", bar.libraryNameForPlatform(android))
        assertEquals(
            listOf(LibraryReference.Literal("-landroid")),
            bar.linkLibsForPlatform(android)
        )

        assertEquals("baz", baz.name)
        assertEquals(packagePath.resolve("modules/baz"), baz.path)
        assertEquals("libbaz", baz.libraryNameForPlatform(android))
        assertEquals(
            listOf(
                LibraryReference.Literal("-llog"),
                LibraryReference.Local("bar"),
                LibraryReference.External("qux", "libqux")
            ), baz.linkLibsForPlatform(android)
        )
    }

    @Test
    fun `package with unsupported platforms does not load`() {
        assertFailsWith(UnsupportedPlatformException::class) {
            val packagePath =
                Paths.get(
                    this.javaClass.getResource(
                        "packages/unsupported_platform"
                    ).toURI()
                )
            Package(packagePath)
        }
    }

    @Test
    fun `package with missing artifact id does not load`() {
        assertFailsWith(MissingArtifactIDException::class) {
            val packagePath =
                Paths.get(
                    this.javaClass.getResource(
                        "packages/missing_artifact_id"
                    ).toURI()
                )
            Package(packagePath)
        }
    }

    @Test
    fun `package with schema version other than 1 is rejected`() {
        assertFailsWith(IllegalArgumentException::class) {
            val packagePath =
                Paths.get(
                    this.javaClass.getResource(
                        "packages/wrong_schema_version"
                    ).toURI()
                )
            Package(packagePath)
        }
    }
}
