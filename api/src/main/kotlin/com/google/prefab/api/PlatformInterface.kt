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
 * Defines the platform-specific information that is used to determine
 * compatibility between user requirements and prebuilt libraries.
 */
interface PlatformDataInterface {
    /**
     * Determines if the given [requirement] can be used with this platform.
     *
     * This [PlatformDataInterface] object defines the platform requirements
     * specified by the user. The given [requirement] object defines the
     * requirements of a prebuilt library found in the module. This function
     * returns true if the library can be used given the user's requirements.
     *
     * @param[requirement] The [platform requirements][PlatformDataInterface]
     * for a library to be checked for compatibility.
     * @return true if the given [requirement] is compatible with this
     * [PlatformDataInterface] platform.
     */
    fun canUse(requirement: PlatformDataInterface): Boolean

    /**
     * Returns the library file found in the given directory.
     *
     * @param[directory] The path to the library directory.
     * @param[module] The module the library belongs to.
     * @throws[RuntimeException] No unique library could be found in the
     * directory.
     * @return A [Path] referring to the prebuilt library for the given
     * directory and module name.
     */
    fun libraryFileFromDirectory(directory: Path, module: Module): Path
}
