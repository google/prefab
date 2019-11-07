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
 * An interface for factories that create [PlatformDataInterface] objects.
 */
interface PlatformFactoryInterface {
    /**
     * The library directory platform name handled by the derived class.
     *
     * Platform specific library directories are named with a tuple of the
     * associated identifier and a string with platform-specific meaning, joined
     * by a `.`. For example, 64-bit Arm Android libraries are in the
     * `android.arm64-v8a` directory.
     */
    val identifier: String

    /**
     * Constructs a [PlatformDataInterface] describing the library contained in
     * [directory].
     *
     * @param[directory] The library directory.
     * @return A [PlatformDataInterface] describing the library.
     */
    fun fromLibraryDirectory(directory: Path): PlatformDataInterface
}
