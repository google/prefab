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

import kotlinx.serialization.Serializable

/**
 * The Android abi.json schema.
 *
 * @property[abi] The ABI name of the described library. For a list of valid ABI
 * names, see [Android.Abi].
 * @property[api] The minimum OS version supported by the library. i.e. the
 * library's `minSdkVersion`.
 * @property[ndk] The major version of the NDK that this library was built with.
 * @property[stl] The STL that this library was built with.
 */
@Serializable
data class AndroidAbiMetadata(
    val abi: String,
    val api: Int,
    val ndk: Int,
    val stl: String
)
