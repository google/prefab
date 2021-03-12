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

package com.google.prefab.cmake

import com.google.prefab.api.Android
import com.google.prefab.api.BuildSystemInterface
import com.google.prefab.api.LibraryReference
import com.google.prefab.api.Module
import com.google.prefab.api.NoMatchingLibraryException
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import java.io.File
import java.nio.file.Path

/**
 * Sanitizes a path for use in a CMake file.
 *
 * Convert backslash separated paths to forward slash separated paths on
 * Windows. Even on Windows, it's the norm to use forward slash separated paths
 * for ndk-build.
 *
 * TODO: Figure out if we should be using split/join instead.
 * It's not clear how that will behave with Windows \\?\ paths.
 */
fun Path.sanitize(): String = toString().replace('\\', '/')

/**
 * The build plugin for [CMake](https://cmake.org/).
 */
class CMakePlugin(
    override val outputDirectory: File,
    override val packages: List<Package>
) : BuildSystemInterface {
    override fun generate(requirements: Collection<PlatformDataInterface>) {
        val requirement = requirements.singleOrNull()
            ?: throw UnsupportedOperationException(
                "CMake cannot generate multiple targets to a single directory"
            )

        prepareOutputDirectory(outputDirectory)

        for (pkg in packages) {
            generatePackage(pkg, requirement)
        }
    }

    private fun generatePackage(
        pkg: Package,
        requirements: PlatformDataInterface
    ) {
        // Until r19 the NDK CMake toolchain file did not properly set
        // CMAKE_LIBRARY_ARCHITECTURE, so we can't use the architecture-specific
        // layout unless targeting at least r19.
        val useArchSpecificLayout =
            requirements !is Android || requirements.ndkMajorVersion >= 19

        // https://cmake.org/cmake/help/latest/command/find_package.html#search-procedure
        // We could also include a version number suffix on the package name
        // part of the directory, but since we don't support duplicate package
        // names in the same run there's currently no chance of a conflict.
        val pkgDirectory = if (useArchSpecificLayout) {
            outputDirectory.resolve(
                "lib/${requirements.targetTriple}/cmake/${pkg.name}"
            ).apply { mkdirs() }
        } else {
            outputDirectory
        }
        val configFile = pkgDirectory.resolve("${pkg.name}Config.cmake")
        for (dep in pkg.dependencies.sorted()) {
            emitDependency(dep, configFile)
        }

        for (module in pkg.modules.sortedBy { it.name }) {
            emitModule(pkg, module, requirements, configFile)
        }


        if (pkg.version != null) {
            emitVersionFile(
                pkg,
                pkgDirectory.resolve("${pkg.name}ConfigVersion.cmake")
            )
        }
    }

    private fun emitDependency(dep: String, configFile: File) {
        configFile.appendText(
            """
            find_package($dep REQUIRED CONFIG)


        """.trimIndent()
        )
    }

    private fun emitModule(
        pkg: Package,
        module: Module,
        requirements: PlatformDataInterface,
        configFile: File
    ) {

        val ldLibs =
            module.linkLibsForPlatform(requirements)
                .filterIsInstance<LibraryReference.Literal>().map { it.arg }

        val localReferences =
            module.linkLibsForPlatform(requirements)
                .filterIsInstance<LibraryReference.Local>()
                .map { "${pkg.name}::${it.name}" }

        val externalReferences =
            module.linkLibsForPlatform(requirements)
                .filterIsInstance<LibraryReference.External>()
                .map { "${it.pkg}::${it.module}" }

        // TODO: Can we be more careful about ordering?
        val libraries =
            (ldLibs + localReferences + externalReferences).joinToString(";")

        val target = "${pkg.name}::${module.name}"
        if (module.isHeaderOnly) {
            val escapedHeaders = module.includePath.sanitize()
            configFile.appendText(
                """
                if(NOT TARGET $target)
                add_library($target INTERFACE IMPORTED)
                set_target_properties($target PROPERTIES
                    INTERFACE_INCLUDE_DIRECTORIES "$escapedHeaders"
                    INTERFACE_LINK_LIBRARIES "$libraries"
                )
                endif()


                """.trimIndent()
            )
        } else {
            try {
                val prebuilt = module.getLibraryFor(requirements)
                val escapedLibrary = prebuilt.path.sanitize()
                val escapedHeaders = prebuilt.includePath.sanitize()
                val prebuiltType: String =
                    when (val extension = prebuilt.path.toFile().extension) {
                        "so" -> "SHARED"
                        "a" -> "STATIC"
                        else -> throw RuntimeException(
                            "Unrecognized library extension: $extension"
                        )
                    }

                configFile.appendText(
                    """
                    if(NOT TARGET $target)
                    add_library($target $prebuiltType IMPORTED)
                    set_target_properties($target PROPERTIES
                        IMPORTED_LOCATION "$escapedLibrary"
                        INTERFACE_INCLUDE_DIRECTORIES "$escapedHeaders"
                        INTERFACE_LINK_LIBRARIES "$libraries"
                    )
                    endif()


                    """.trimIndent()
                )
            } catch (ex: NoMatchingLibraryException) {
                // Libraries that do not match our requirements should be logged
                // and ignored.
                System.err.println(ex)
            }
        }
    }

    private fun emitVersionFile(pkg: Package, versionFile: File) {
        // https://cmake.org/cmake/help/latest/manual/cmake-packages.7.html#package-version-file
        // Based on example from
        // https://gitlab.kitware.com/cmake/community/wikis/doc/tutorials/How-to-create-a-ProjectConfig.cmake-file
        versionFile.writeText(
            """
            set(PACKAGE_VERSION ${pkg.version})
            if("${'$'}{PACKAGE_VERSION}" VERSION_LESS "${'$'}{PACKAGE_FIND_VERSION}")
                set(PACKAGE_VERSION_COMPATIBLE FALSE)
            else()
                set(PACKAGE_VERSION_COMPATIBLE TRUE)
                if("${'$'}{PACKAGE_VERSION}" VERSION_EQUAL "${'$'}{PACKAGE_FIND_VERSION}")
                    set(PACKAGE_VERSION_EXACT TRUE)
                endif()
            endif()
            """.trimIndent()
        )
    }
}