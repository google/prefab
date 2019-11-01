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

import java.io.File

/**
 * The interface that Prefab uses to create build system plugins.
 */
interface BuildSystemFactory {
    /**
     * An identifier matching the build system.
     *
     * This value is used to identify the correct build system plugin based on
     * the --build-system argument from the user.
     */
    val identifier: String

    /**
     * Creates the associated [BuildSystemInterface].
     *
     * @param[outputDirectory] The directory that build system integration
     * should be generated to.
     * @param[packages] The list of packages to generate build system
     * integrations for.
     * @return A newly constructed [BuildSystemInterface].
     */
    fun create(
        outputDirectory: File,
        packages: List<Package>
    ): BuildSystemInterface
}

/**
 * An interface to a build system generator.
 *
 * This, along with the associated [BuildSystemFactory] is the interface that
 * build system plugins implement.
 */
interface BuildSystemInterface {
    /**
     * The directory that build system integration should be generated to.
     */
    val outputDirectory: File

    /**
     * The list of packages to generate build system integrations for.
     */
    val packages: List<Package>

    /**
     * Generates build system integration for the given [packages] to the
     * [outputDirectory], for each of the specified [PlatformDataInterface]
     * requirements.
     *
     * @param[requirements] A list of target configurations requested by the
     * user.
     */
    fun generate(requirements: Collection<PlatformDataInterface>)

    /**
     * Ensures that the given [output] directory exists and is empty.
     *
     * @param[output] The directory to prepare.
     */
    fun prepareOutputDirectory(output: File) {
        if (output.exists()) {
            output.deleteRecursively()
        }
        output.mkdirs()
    }
}
