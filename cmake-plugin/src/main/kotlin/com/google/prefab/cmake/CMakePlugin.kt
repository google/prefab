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

import com.google.prefab.api.BuildSystemFactory
import com.google.prefab.api.BuildSystemInterface
import com.google.prefab.api.LibraryReference
import com.google.prefab.api.Module
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import java.io.File

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
        // https://cmake.org/cmake/help/latest/command/find_package.html#search-procedure
        //
        // The share/$name directory is probably the best fit, but that doesn't
        // appear to work with CMake 3.6, which the NDK still supports.
        //
        // TODO: Use the arch-specific directory?
        // Doing so would allow a single output directory to be used for all
        // ABIs.
        val configFile = outputDirectory.resolve("${pkg.name}-config.cmake")
        for (dep in pkg.dependencies.sorted()) {
            emitDependency(dep, configFile)
        }

        for (module in pkg.modules.sortedBy { it.name }) {
            emitModule(pkg, module, requirements, configFile)
        }


        if (pkg.version != null) {
            emitVersionFile(
                pkg,
                outputDirectory.resolve("${pkg.name}-config-version.cmake")
            )
        }
    }

    private fun emitDependency(dep: String, configFile: File) {
        configFile.appendText(
            """
            find_package($dep REQUIRED)


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
            configFile.appendText(
                """
                add_library($target INTERFACE)
                set_target_properties($target PROPERTIES
                    INTERFACE_INCLUDE_DIRECTORIES "${module.includePath}"
                    INTERFACE_LINK_LIBRARIES "$libraries"
                )
    
    
                """.trimIndent()
            )
        } else {
            val prebuilt =
                module.getLibraryFor(requirements) ?: throw RuntimeException(
                    "No library found matching $requirements for " +
                            "${module.canonicalName} and module is not header " +
                            "only."
                )

            configFile.appendText(
                """
                add_library($target SHARED IMPORTED)
                set_target_properties($target PROPERTIES
                    IMPORTED_LOCATION "${prebuilt.path}"
                    INTERFACE_INCLUDE_DIRECTORIES "${prebuilt.includePath}"
                    INTERFACE_LINK_LIBRARIES "$libraries"
                )


                """.trimIndent()
            )
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

    /**
     * The [CMakePlugin] factory object.
     */
    companion object : BuildSystemFactory {
        override val identifier: String = "cmake"

        override fun create(
            outputDirectory: File,
            packages: List<Package>
        ): BuildSystemInterface {
            return CMakePlugin(outputDirectory, packages)
        }
    }
}
