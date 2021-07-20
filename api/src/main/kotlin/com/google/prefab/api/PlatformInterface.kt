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

/**
 * Result of prebuilt library usability check.
 */
sealed class LibraryUsabilityResult

/**
 * The library is usable.
 */
object CompatibleLibrary : LibraryUsabilityResult()

/**
 * The library is not usable.
 *
 * @property[reason] The reason the library was rejected to be shown to the
 * user.
 */
data class IncompatibleLibrary(val reason: String) : LibraryUsabilityResult()

/**
 * Defines the platform-specific information that is used to determine
 * compatibility between user requirements and prebuilt libraries.
 */
interface PlatformDataInterface {
    /**
     * The target triple used for this platform.
     *
     * This is used to determine the architecture-specific directory for
     * installing CMake build scripts, so it must match the platform's
     * [CMAKE_LIBRARY_ARCHITECTURE](https://cmake.org/cmake/help/latest/variable/CMAKE_LIBRARY_ARCHITECTURE.html).
     */
    val targetTriple: String

    /**
     * Determines if the given [library] can be used with this platform.
     *
     * This [PlatformDataInterface] object defines the platform requirements
     * specified by the user. This function returns true if the [library] can be
     * used given the user's requirements.
     *
     * @param[library] The library to be checked for compatibility.
     * @return A [LibraryUsabilityResult] describing if the [library] is usable
     * with this [platform][PlatformDataInterface], and if not explains why the
     * [library] was rejected.
     */
    fun checkIfUsable(library: PrebuiltLibrary): LibraryUsabilityResult

    /**
     * Finds the best fit library for these platform requirements.
     *
     * @param[libraries] A non-empty list of compatible libraries.
     * @throws[IllegalArgumentException] The given list of [libraries] was
     * either empty or incompatible with this platform.
     * @throws[RuntimeException] 
     * @return The [PrebuiltLibrary] from the set of [libraries] that is the
     * best fit for these platform requirements.
     */
    fun findBestMatch(libraries: List<PrebuiltLibrary>): PrebuiltLibrary
}
