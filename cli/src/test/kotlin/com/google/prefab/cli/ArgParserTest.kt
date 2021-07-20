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

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingArgument
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.UsageError
import com.google.prefab.api.SchemaVersion
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(Parameterized::class)
class ArgParserTest(override val schemaVersion: SchemaVersion) : PerSchemaTest {
    class NoRunTestCli : Cli() {
        override fun run() {
            validate()
        }
    }

    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

    private val file: File = File.createTempFile("tmpfile", null).apply {
        deleteOnExit()
    }

    private val fooPath: Path = packagePath("foo")
    private val quxPath: Path = packagePath("qux")

    @Test
    fun `fails if --build-system is missing`() {
        assertFailsWith(MissingOption::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--output",
                    "out",
                    fooPath.toString()
                )
            )
        }
    }

    @Test
    fun `fails if --output is missing`() {
        assertFailsWith(MissingOption::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--build-system", "ndk-build",
                    fooPath.toString()
                )
            )
        }
    }

    @Test
    fun `fails if --output is a file`() {
        assertFailsWith(BadParameterValue::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--build-system", "ndk-build",
                    "--output", file.path,
                    fooPath.toString()
                )
            )
        }
    }

    @Test
    fun `fails if platform is not recognized`() {
        assertFailsWith(UsageError::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "unknown",
                    "--build-system", "ndk-build",
                    "--output", "out",
                    "foo"
                )
            )
        }
    }

    @Test
    fun `fails if package path doesn't exist`() {
        assertFailsWith(BadParameterValue::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--ndk-version", "21",
                    "--build-system", "ndk-build",
                    "--output", "out",
                    "foo"
                )
            )
        }
    }

    @Test
    fun `fails if package path is a file`() {
        assertFailsWith(BadParameterValue::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--ndk-version", "21",
                    "--build-system", "ndk-build",
                    "--output", "out",
                    file.path
                )
            )
        }
    }

    @Test
    fun `fails if no packages are provided`() {
        assertFailsWith(MissingArgument::class) {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--ndk-version", "21",
                    "--build-system", "ndk-build",
                    "--output", "out"
                )
            )
        }
    }

    @Test
    fun `no errors with valid arguments`() {
        NoRunTestCli().parse(
            listOf(
                "--platform", "android",
                "--abi", "arm64-v8a",
                "--os-version", "21",
                "--stl", "c++_shared",
                "--ndk-version", "21",
                "--build-system", "ndk-build",
                "--output", "out",
                fooPath.toString(),
                quxPath.toString()
            )
        )
    }

    @Test
    fun `duplicate packages names are an error`() {
        val packageA = packagePath("duplicate_package_names_a")
        val packageB = packagePath("duplicate_package_names_b")

        assertFailsWith<DuplicatePackageNamesException> {
            NoRunTestCli().parse(
                listOf(
                    "--platform", "android",
                    "--abi", "arm64-v8a",
                    "--os-version", "21",
                    "--stl", "c++_shared",
                    "--ndk-version", "21",
                    "--build-system", "ndk-build",
                    "--output", "out",
                    packageA.toString(),
                    packageB.toString()
                )
            )
        }
    }
}
