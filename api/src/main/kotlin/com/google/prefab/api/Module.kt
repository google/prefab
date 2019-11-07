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
import java.nio.file.Path

/**
 * The module contains a library that targets an unsupported platform.
 *
 * @param[module] The module with the unrecognized platform.
 * @param[platformName] The name of the unrecognized platform.
 */
class UnsupportedPlatformException(module: Module, platformName: String) :
    Exception(
        "${module.canonicalName} contains artifacts for unsupported platform " +
                "\"$platformName\""
    )

/**
 * The module contains a library directory with an invalid name.
 *
 * @param[module] The module with the invalid library directory.
 * @param[artifactDirectory] The library directory with the invalid name.
 */
class MissingArtifactIDException(module: Module, artifactDirectory: Path) :
    Exception(
        "${module.canonicalName} artifact directory has invalid name: " +
                artifactDirectory
    )

/**
 * A Prefab module.
 *
 * @property[path] The path to the module directory.
 * @property[pkg] The [Package] this module belongs to.
 */
class Module(val path: Path, val pkg: Package) {
    /**
     * The metadata object loaded form module.json.
     */
    private val metadata: ModuleMetadataV1 = Json.parse(
        path.resolve("module.json").toFile().readText()
    )

    /**
     * The name of the module.
     */
    val name: String = path.fileName.toString()

    /**
     * The fully qualified name of this module, which includes the package name.
     *
     * This name is also the name that can be used with an
     * [external library reference][LibraryReference.External].
     */
    val canonicalName: String = "//${pkg.name}/$name"

    /**
     * The include directory that should be exported to dependents.
     *
     * Note that this should only be used by build plugins for header-only
     * modules. If the module is not header only, [PrebuiltLibrary.includePath]
     * should be used instead as the include directory may change per-platform.
     */
    val includePath: Path = path.resolve("include")

    /**
     * The list of [prebuilt libraries][PrebuiltLibrary] in this module.
     */
    val libraries: List<PrebuiltLibrary> =
        path.resolve("libs").toFile().listFiles()?.map { directory ->
            val basename = directory.toPath().fileName.toString()
            val components = basename.split(".", limit = 2)
            if (components.size != 2) {
                throw MissingArtifactIDException(this, directory.toPath())
            }

            val (platformName, _) = components

            val platformFactory = PlatformRegistry.find(platformName)
                ?: throw UnsupportedPlatformException(this, platformName)

            PrebuiltLibrary(
                directory.toPath(),
                this,
                platformFactory.fromLibraryDirectory(directory.toPath())
            )
        } ?: emptyList()

    /**
     * True if the module is header only.
     */
    val isHeaderOnly: Boolean = libraries.isEmpty()

    /**
     * Finds the library matching the given
     * [platform requirements][PlatformDataInterface].
     *
     * Note that currently this returns the *first* match in an arbitrary sort
     * order. Later this will be updated to return the best match, if there is
     * more than one compatible library.
     *
     * @param[platformData] The build requirements to find a library for.
     * @return The [PrebuiltLibrary] matching the given requirements, or null if
     * there is no match.
     */
    fun getLibraryFor(platformData: PlatformDataInterface): PrebuiltLibrary? {
        // TODO: Find best fit.
        // More than one library might satisfy the requirements.
        return libraries.find { platformData.canUse(it) }
    }

    /**
     * Get libraries that should be exported to dependents.
     *
     * @param[platform] The platform to query the libraries for.
     * @throws[IllegalArgumentException] The given platform is not recognized.
     * @return The libraries appropriate for the given platform.
     */
    fun linkLibsForPlatform(platform: PlatformDataInterface):
            List<LibraryReference> {
        val platformSpecificLibs = when (platform) {
            is Android -> metadata.android.exportLibraries
            else -> throw IllegalArgumentException(
                "Unrecognized platform: $platform"
            )
        }

        return platformSpecificLibs?.map { LibraryReference.fromString(it) }
            ?: metadata.exportLibraries.map { LibraryReference.fromString(it) }
    }

    /**
     * Get name of the library file.
     *
     * @param[platform] The platform to query the library name for.
     * @throws[IllegalArgumentException] The given platform is not recognized.
     * @return The library name for the given platform.
     */
    fun libraryNameForPlatform(platform: PlatformDataInterface): String {
        return when (platform) {
            is Android -> metadata.android.libraryName
            else -> throw IllegalArgumentException(
                "Unrecognized platform: $platform"
            )
        } ?: metadata.libraryName ?: "lib$name"
    }
}
