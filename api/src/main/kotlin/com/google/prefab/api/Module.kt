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

import java.nio.file.Path

/**
 * The module contains a library that targets an unsupported platform.
 *
 * @param[module] The module with the unrecognized platform.
 * @param[platformName] The name of the unrecognized platform.
 */
class UnsupportedPlatformException(module: Module, platformName: String) :
    Exception(
        "${module.canonicalName} contains artifacts for an unsupported " +
                "platform \"$platformName\""
    )

/**
 * The module contains a library directory which does not contain a platform ID.
 *
 * @param[module] The module with the library directory which is missing the
 * platform ID.
 * @param[artifactDirectory] The library directory which is missing the platform
 * ID.
 */
class MissingPlatformIDException(module: Module, artifactDirectory: Path) :
    Exception(
        "${module.canonicalName} artifact directory $artifactDirectory"
                + " does not contain a platform ID. It should have the name"
                + " format <platform ID>.<artifact ID> e.g. android.x86"
    )

/**
 * The module contains a library directory which does not contain an artifact
 * ID.
 *
 * @param[module] The module with the library directory which is missing the
 * artifact ID.
 * @param[artifactDirectory] The library directory which is missing the artifact
 * ID.
 */
class MissingArtifactIDException(module: Module, artifactDirectory: Path) :
    Exception(
        "${module.canonicalName} artifact directory $artifactDirectory"
                + " is missing an artifact ID. It should have the name"
                + " format <platform ID>.<artifact ID> e.g. android.x86"
    )

/**
 * The module contains a library directory with an invalid name.
 *
 * @param[module] The module with the invalid library directory.
 * @param[artifactDirectory] The library directory with the invalid name.
 */
class InvalidDirectoryNameException(
    module: Module, artifactDirectory: Path
) : Exception(
    "${module.canonicalName} artifact directory $artifactDirectory"
            + " has an invalid name. It should have the name "
            + " format <platform ID>.<artifact ID> e.g. android.x86"
)

/**
 * The module does not contain a library compatible with the user's
 * requirements.
 *
 * @param[module] The module being searched.
 * @param[rejectedLibraries] A map from the rejected libraries to reasons for
 * their rejection.
 */
class NoMatchingLibraryException(
    module: Module, rejectedLibraries: Map<PrebuiltLibrary, String>
) : Exception(
    "No compatible library found for ${module.canonicalName}. Rejected the "
            + "following libraries:\n"
            + rejectedLibraries.map {
        "${it.key.path.parent.fileName}: ${it.value}"
    }.joinToString("\n")
)

/**
 * A Prefab module.
 *
 * @property[path] The path to the module directory.
 * @property[pkg] The [Package] this module belongs to.
 * @param[loadSchemaVersion] The schema version of the package being loaded.
 */
class Module(
    val path: Path,
    val pkg: Package,
    loadSchemaVersion: SchemaVersion
) {
    /**
     * The metadata object loaded form module.json.
     */
    internal val metadata: ModuleMetadataV1 =
        ModuleMetadata.loadAndMigrate(loadSchemaVersion, path)

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
                throw InvalidDirectoryNameException(this, directory.toPath())
            }

            if (components[0].isEmpty()) {
                throw MissingPlatformIDException(this, directory.toPath())
            }

            if (components[1].isEmpty()) {
                throw MissingArtifactIDException(this, directory.toPath())
            }

            val (platformName, _) = components

            val platformFactory = PlatformRegistry.find(platformName)
                ?: throw UnsupportedPlatformException(this, platformName)

            platformFactory.prebuiltLibraryFromDirectory(
                directory.toPath(),
                this,
                loadSchemaVersion
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
     * If more than one library is a valid match, the best match will be
     * returned. The criteria for finding the best match are defined by
     * [PlatformDataInterface.findBestMatch].
     *
     * @param[platformData] The build requirements to find a library for.
     * @throws[NoMatchingLibraryException] No library compatible with
     * [platformData] was found. Incompatible modules should typically be logged
     * and skipped by the build system plugin.
     * @return The [PrebuiltLibrary] matching the given requirements.
     */
    fun getLibraryFor(platformData: PlatformDataInterface): PrebuiltLibrary {
        val compatible = mutableListOf<PrebuiltLibrary>()
        val rejections = mutableMapOf<PrebuiltLibrary, String>()
        for (library in libraries) {
            when (val result = platformData.checkIfUsable(library)) {
                is CompatibleLibrary -> compatible.add(library)
                is IncompatibleLibrary -> rejections[library] = result.reason
            }
        }
        if (compatible.isEmpty()) {
            // TODO: https://github.com/google/prefab/issues/107
            // As currently implemented it is up to each build system plugin
            // whether or not missing matches are skipped or an error.
            //
            // Some refactoring of the build system plugin API could make it the
            // CLI's responsibility to set that policy and print the results.
            throw NoMatchingLibraryException(
                this,
                rejections.toSortedMap(compareBy { it.path.parent.fileName })
            )
        }
        return platformData.findBestMatch(compatible)
    }

    /**
     * Get libraries that should be exported to dependents.
     *
     * @param[platform] The platform to query the libraries for.
     * @throws[IllegalArgumentException] The given platform is not recognized.
     * @return The libraries appropriate for the given platform.
     */
    fun linkLibsForPlatform(platform: PlatformDataInterface): List<LibraryReference> {
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
