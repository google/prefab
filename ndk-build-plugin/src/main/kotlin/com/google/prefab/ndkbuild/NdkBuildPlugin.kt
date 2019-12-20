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

package com.google.prefab.ndkbuild

import com.google.prefab.api.Android
import com.google.prefab.api.BuildSystemInterface
import com.google.prefab.api.LibraryReference
import com.google.prefab.api.Module
import com.google.prefab.api.Package
import com.google.prefab.api.PlatformDataInterface
import java.io.File
import java.nio.file.Path

/**
 * Sanitizes a path for use in an Android.mk file.
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
 * The requested packages are mutually incompatible in ndk-build because module
 * names are not unique across all packages.
 */
class DuplicateModuleNameException(a: Module, b: Module) : RuntimeException(
    "Duplicate module name found (${a.canonicalName} and " +
            "${b.canonicalName}). ndk-build does not support fully qualified " +
            "module names."
)

/**
 * The build plugin for
 * [ndk-build](https://developer.android.com/ndk/guides/ndk-build).
 */
class NdkBuildPlugin(
    override val outputDirectory: File,
    override val packages: List<Package>
) : BuildSystemInterface {
    override fun generate(requirements: Collection<PlatformDataInterface>) {
        val androidRequirements =
            requirements.filterIsInstance<Android>()
        if (androidRequirements != requirements) {
            throw UnsupportedOperationException(
                "ndk-build only supports Android targets"
            )
        }

        // ndk-build does not support fully-qualified module names, so verify
        // that module names are unique across all packages.
        val seenNames = mutableMapOf<String, Module>()
        for (pkg in packages) {
            for (module in pkg.modules) {
                val dupModule = seenNames[module.name]
                if (dupModule != null) {
                    throw DuplicateModuleNameException(module, dupModule)
                }

                seenNames[module.name] = module
            }
        }

        prepareOutputDirectory(outputDirectory)

        for (pkg in packages) {
            val packageDir = outputDirectory.resolve(pkg.name)
            packageDir.mkdir()
            generatePackage(pkg, androidRequirements, packageDir)
        }
    }

    private fun generatePackage(
        pkg: Package,
        requirements: Collection<Android>,
        packageDirectory: File
    ) {
        val androidMk = packageDirectory.resolve("Android.mk")
        androidMk.writeText(
            """
            LOCAL_PATH := $(call my-dir)


            """.trimIndent()
        )
        for (requirement in requirements) {
            androidMk.appendText(
                """
                ifeq ($(TARGET_ARCH_ABI),${requirement.abi.targetArchAbi})


                """.trimIndent()
            )

            for (module in pkg.modules.sortedBy { it.name }) {
                emitModule(module, requirement, androidMk)
            }

            androidMk.appendText(
                """
                endif  # ${requirement.abi.targetArchAbi}


                """.trimIndent()
            )
        }

        for (dep in pkg.dependencies.sorted()) {
            emitDependency(dep, androidMk)
        }
    }

    private fun emitModule(
        module: Module,
        requirement: Android,
        androidMk: File
    ) {
        val ldLibs = mutableListOf<String>()
        val sharedLibraries = mutableListOf<String>()
        val staticLibraries = mutableListOf<String>()

        for (reference in module.linkLibsForPlatform(requirement)) {
            if (reference is LibraryReference.Literal) {
                ldLibs.add(reference.arg)
            } else {
                val referredModule = findReferredModule(reference, module)
                if (referredModule.isHeaderOnly) {
                    staticLibraries.add(referredModule.name)
                } else {
                    val prebuilt = referredModule.getLibraryFor(requirement)
                    when (val extension = prebuilt.path.toFile().extension) {
                        "so" -> sharedLibraries.add(referredModule.name)
                        "a" -> staticLibraries.add(referredModule.name)
                        else -> throw RuntimeException(
                            "Unrecognized library extension: $extension"
                        )
                    }
                }
            }
        }

        val exportLdLibs =
            ldLibs.joinToString(" ", prefix = " ").trimEnd()

        val exportSharedLibraries =
            sharedLibraries.joinToString(" ", prefix = " ").trimEnd()

        val exportStaticLibraries =
            staticLibraries.joinToString(" ", prefix = " ").trimEnd()


        if (module.isHeaderOnly) {
            // ndk-build doesn't have an explicit header-only library type; it's
            // just a static library with no sources.
            val escapedHeaders = module.includePath.sanitize()
            androidMk.appendText(
                """
                include $(CLEAR_VARS)
                LOCAL_MODULE := ${module.name}
                LOCAL_EXPORT_C_INCLUDES := $escapedHeaders
                LOCAL_EXPORT_SHARED_LIBRARIES :=$exportSharedLibraries
                LOCAL_EXPORT_STATIC_LIBRARIES :=$exportStaticLibraries
                LOCAL_EXPORT_LDLIBS :=$exportLdLibs
                include $(BUILD_STATIC_LIBRARY)
    
    
                """.trimIndent()
            )
        } else {
            val prebuilt = module.getLibraryFor(requirement)
            val escapedLibrary = prebuilt.path.sanitize()
            val escapedHeaders = prebuilt.includePath.sanitize()
            val prebuiltType: String =
                when (val extension = prebuilt.path.toFile().extension) {
                    "so" -> "PREBUILT_SHARED_LIBRARY"
                    "a" -> "PREBUILT_STATIC_LIBRARY"
                    else -> throw RuntimeException(
                        "Unrecognized library extension: $extension"
                    )
                }

            androidMk.appendText(
                """
                include $(CLEAR_VARS)
                LOCAL_MODULE := ${module.name}
                LOCAL_SRC_FILES := $escapedLibrary
                LOCAL_EXPORT_C_INCLUDES := $escapedHeaders
                LOCAL_EXPORT_SHARED_LIBRARIES :=$exportSharedLibraries
                LOCAL_EXPORT_STATIC_LIBRARIES :=$exportStaticLibraries
                LOCAL_EXPORT_LDLIBS :=$exportLdLibs
                include $($prebuiltType)
    
    
                """.trimIndent()
            )
        }
    }

    private fun emitDependency(dependency: String, androidMk: File) {
        androidMk.appendText(
            """
            $(call import-module,prefab/$dependency)

            """.trimIndent()
        )
    }

    /**
     * Finds the module matching a given library reference.
     */
    private fun findReferredModule(
        reference: LibraryReference,
        currentModule: Module
    ): Module = when (reference) {
        is LibraryReference.Local -> packages.find {
            it.name == currentModule.pkg.name
        }?.modules?.find { it.name == reference.name }
        is LibraryReference.External -> packages.find {
            it.name == reference.pkg
        }?.modules?.find { it.name == reference.module }
        is LibraryReference.Literal -> throw IllegalArgumentException(
            "Literal library references do not have types"
        )
    } ?: throw RuntimeException(
        "Could not find a module matching $reference"
    )
}
