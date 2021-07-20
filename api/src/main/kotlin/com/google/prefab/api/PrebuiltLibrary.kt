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
 * A platform-specific library file in a module.
 *
 * @property[path] The path to the library file on disk.
 * @property[module]: The module associated with this library.
 * @property[platform] The [platform-specific data][PlatformDataInterface]
 * describing the library.
 */
data class PrebuiltLibrary(
    val path: Path,
    val module: Module,
    val platform: PlatformDataInterface,
) {
    /**
     * The platform-specific library directory containing the library file.
     */
    val directory: Path = path.parent

    /**
     * The path to the headers on disk.
     *
     * This is the path to the platform-specific include path if it exists.
     * Otherwise this is the path to the module's include path.
     */
    val includePath: Path =
        directory.resolve("include").takeIf { it.toFile().exists() }
            ?: module.includePath
}
