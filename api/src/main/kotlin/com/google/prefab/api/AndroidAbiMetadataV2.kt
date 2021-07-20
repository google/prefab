/*
 * Copyright 2021 Google LLC
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path

/**
 * The V2 Android abi.json schema.
 *
 * Differs from the V1 schema with the `static` property in the JSON file. This
 * property is used to determine whether the library in the directory is a
 * static or shared library. In the V1 schema this was determined based on the
 * directory contents, but this means that the directory could not be processed
 * until the library was built. This was fine for the typical case where the
 * libraries are actually prebuilt, but Android Studio plans to use Prefab as
 * the intermediate format for passing build outputs between externalNativeBuild
 * modules. Since the IDE needs to be able to run the CMake configuration for
 * all modules before *any* module is built, the libraries will not be present
 * yet.
 *
 * @property[abi] The ABI name of the described library. For a list of valid ABI
 * names, see [Android.Abi].
 * @property[api] The minimum OS version supported by the library. i.e. the
 * library's `minSdkVersion`.
 * @property[ndk] The major version of the NDK that this library was built with.
 * @property[stl] The STL that this library was built with.
 * @property[isStatic] True if the library is a static library. Defaults to
 * false.
 */
@Serializable
data class AndroidAbiMetadataV2(
    val abi: String,
    val api: Int,
    val ndk: Int,
    val stl: String,
    @SerialName("static")
    val isStatic: Boolean = false,
) : AndroidAbiMetadata {
    override fun migrate(
        module: Module,
        directory: Path
    ): AndroidAbiMetadataV2 = copy()

    companion object : VersionedMetadataLoader<AndroidAbiMetadataV2> {
        override fun load(directory: Path): AndroidAbiMetadataV2 =
            Json.decodeFromString(
                directory.toFile().resolve("abi.json").readText()
            )
    }
}
