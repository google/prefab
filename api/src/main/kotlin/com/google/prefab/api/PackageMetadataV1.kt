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
 * The v1 package.json schema.
 *
 * @property[name] The name of the module.
 * @property[schemaVersion] The version of the schema. Must be 1.
 * @property[dependencies] A list of other packages required by this package.
 * @property[version] The package version. For compatibility with CMake, this
 * *must* be formatted as major[.minor[.patch[.tweak]]] with all components
 * being numeric, even if that does not match the package's native version.
 */
@Serializable
data class PackageMetadataV1(
    val name: String,
    @SerialName("schema_version")
    val schemaVersion: Int,
    val dependencies: List<String>,
    val version: String? = null
)
