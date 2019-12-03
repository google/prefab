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

import com.google.prefab.api.BuildSystemProvider
import java.util.ServiceLoader

/**
 * A list of known build systems.
 *
 * This is currently just a static list of our explicit dependencies, but before
 * release plugins will be able to register themselves at runtime.
 */
object BuildSystemRegistry {
    /**
     * The list of known build systems.
     */
    private val buildSystems: List<BuildSystemProvider> =
        ServiceLoader.load(BuildSystemProvider::class.java).iterator()
            .asSequence().toList()

    /**
     * Determines whether or not a build system with the given name is
     * supported.
     *
     * @param[identifier] The name of the build system as requested by
     * --build-system.
     * @return true if the given [identifier] matches a supported build system.
     */
    fun supports(identifier: String): Boolean {
        return find(identifier) != null
    }

    /**
     * Finds the build system with the given name.
     *
     * @param[identifier] The name of the build system as requested by
     * --build-system.
     * @return The build system matching [identifier], or null if there is no
     * match.
     */
    fun find(identifier: String): BuildSystemProvider? {
        return buildSystems.find { it.identifier == identifier }
    }
}
