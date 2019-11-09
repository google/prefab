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

import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import java.io.File
import java.nio.file.Path

/**
 * Checks if a version string is in the right format for CMake.
 *
 * @param[version] The version string.
 * @return True if the version string is compatible.
 */
internal fun isValidVersionForCMake(version: String): Boolean =
    Regex("""^\d+(\.\d+(\.\d+(\.\d+)?)?)?$""").matches(version)

/**
 * A Prefab package.
 *
 * @property[path] The path to the package on disk.
 */
class Package(val path: Path) {
    /**
     * The metadata object loaded from the prefab.json.
     */
    private val metadata: PackageMetadataV1 = Json.parse<PackageMetadataV1>(
        path.resolve("prefab.json").toFile().readText()
    ).also {
        require(it.schemaVersion == 1) {
            "Only schema_version 1 is supported. ${it.name} uses version " +
                    "${it.schemaVersion}."
        }
    }

    /**
     * The path to the package's module directory.
     */
    private val moduleDir: File = path.resolve("modules").toFile()

    /**
     * The list of modules in this package.
     */
    val modules: List<Module> =
        moduleDir.listFiles()?.map { Module(it.toPath(), this) }
            ?: throw RuntimeException(
                "Unable to retrieve file list for $moduleDir"
            )

    /**
     * The name of the package.
     */
    val name: String = metadata.name

    /**
     * The list of other packages this package requires.
     */
    val dependencies: List<String> = metadata.dependencies

    /**
     * The version of the package
     */
    val version: String? = metadata.version.also {
        require(it == null || isValidVersionForCMake(it))
    }
}
