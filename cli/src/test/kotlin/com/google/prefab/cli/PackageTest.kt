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
import com.google.prefab.api.InvalidDirectoryNameException
import com.google.prefab.api.LibraryReference
import com.google.prefab.api.MissingArtifactIDException
import com.google.prefab.api.MissingPlatformIDException
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import com.google.prefab.api.SchemaVersion
import com.google.prefab.api.UnsupportedPlatformException
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class PackageTest(override val schemaVersion: SchemaVersion) : PerSchemaTest {
    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

    private val android: PlatformDataInterface =
        Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)

    @Test
    fun `can load basic package`() {
        val packagePath = packagePath("foo")
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
    fun `can load package with unexpected files`() {
        val packagePath = packagePath("has_unexpected_files")
        val pkg = Package(packagePath)
        assertEquals(packagePath, pkg.path)
        assertEquals("has_unexpected_files", pkg.name)
        assertEquals(emptyList(), pkg.dependencies)

        assertEquals(1, pkg.modules.size)

        val bar = pkg.modules.single()

        assertEquals("bar", bar.name)
        assertEquals(packagePath.resolve("modules/bar"), bar.path)
        assertEquals("libbar", bar.libraryNameForPlatform(android))
        assertEquals(emptyList(), bar.linkLibsForPlatform(android))
    }

    @Test
    fun `package with unsupported platforms does not load`() {
        assertFailsWith(UnsupportedPlatformException::class) {
            Package(packagePath("unsupported_platform"))
        }
    }

    @Test
    fun `package with invalid directory name does not load`(){
        assertFailsWith(InvalidDirectoryNameException::class) {
            Package(packagePath("invalid_directory_name"))
        }
    }

    @Test
    fun `package with missing platform id does not load`() {
        assertFailsWith(MissingPlatformIDException::class) {
            Package(packagePath("missing_platform_id"))
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `package with missing artifact id does not load`() {
        // We need a file path of "missing_id/libs/android.", but Windows
        // will fail to clone the repo if a directory ends with ".". To work
        // around this, we save the directory with a windows-friendly way and
        // then generate the rest.
        val tempDirPath = kotlin.io.path.createTempDirectory()

        val tempDirFile = tempDirPath.toFile()
        tempDirFile.deleteOnExit()

        assertFailsWith(MissingArtifactIDException::class) {
            packagePath("missing_artifact_id").toFile().apply {
                copyRecursively(tempDirFile)
            }

            tempDirPath.resolve("modules/missing_id/libs/android.").createDirectories()

            Package(tempDirPath)
        }
    }

    @Test
    fun `package with unsupported schema version is rejected`() {
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

    @Test
    fun `package with invalid package version is rejected`() {
        assertFailsWith(IllegalArgumentException::class) {
            Package(packagePath("bad_package_version"))
        }
    }
}
