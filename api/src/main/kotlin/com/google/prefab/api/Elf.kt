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
 * Returns the single library file found in the given directory.
 *
 * @param[directory] The path to the library directory.
 * @param[name] The name of the the library file without the extension.
 * @throws[RuntimeException] No unique library could be found in the
 * directory.
 * @return A [Path] referring to the prebuilt library for the given
 * directory and module name.
 */
fun findElfLibrary(directory: Path, name: String): Path {
    val possibleLibraries = listOf(
        directory.resolve("$name.a"),
        directory.resolve("$name.so")
    )
    val foundLibraries = possibleLibraries.filter { it.toFile().exists() }

    if (foundLibraries.size > 1) {
        throw RuntimeException(
            "Prebuilt directory contains multiple library " +
                    "artifacts: $directory"
        )
    } else if (foundLibraries.isEmpty()) {
        throw RuntimeException(
            "Prebuilt directory contains no library artifacts: " +
                    directory
        )
    }

    return foundLibraries.single()
}