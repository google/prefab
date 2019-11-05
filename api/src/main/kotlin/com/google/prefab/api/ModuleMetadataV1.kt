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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The module metadata that is overridable per-platform in module.json.
 *
 * @property[exportLibraries] The list of libraries other than the library
 * described by this module that must be linked by users of this module.
 * @property[libraryName] The name (without the file extension) of the library
 * file. The file extension is automatically detected based on the contents of
 * the directory.
 */
@Serializable
data class PlatformSpecificModuleMetadataV1(
    @SerialName("export_libraries")
    val exportLibraries: List<String>? = null,
    @SerialName("library_name")
    val libraryName: String? = null
)

/**
 * The v1 module.json schema.
 *
 * @property[exportLibraries] The list of libraries other than the library
 * described by this module that must be linked by users of this module.
 * @property[libraryName] The name (without the file extension) of the library
 * file. The file extension is automatically detected based on the contents of
 * the directory.
 * @property[android] Android specific values that override the main values if
 * present.
 * @property[gnulinux] GNU/Linux specific values that override the main values
 * if present.
 */
@Serializable
data class ModuleMetadataV1(
    @SerialName("export_libraries")
    val exportLibraries: List<String>,
    @SerialName("library_name")
    val libraryName: String? = null,
    // Allowing per-platform overrides before we support more than a single
    // platform might seem like overkill, but this makes it easier to maintain
    // compatibility for old packages when new platforms are added. If we added
    // this in schema version 2, there would be no way to reliably migrate v1
    // packages because we wouldn't know whether the exported libraries were
    // Android specific or not.
    val android: PlatformSpecificModuleMetadataV1 =
        PlatformSpecificModuleMetadataV1(null, null),
    val gnulinux: PlatformSpecificModuleMetadataV1 =
        PlatformSpecificModuleMetadataV1(null, null)
)
