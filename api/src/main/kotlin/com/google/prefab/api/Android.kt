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

import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import java.nio.file.Path
import kotlin.math.max

/**
 * Platform requirements for Android.
 *
 * @property[abi] The ABI targeted by this build.
 * @property[api] The Android minSdkVersion targeted by this build.
 * @constructor Creates an Android requirements object.
 */
class Android(val abi: Abi, api: Int) : PlatformDataInterface {
    val api: Int = when (abi) {
        Abi.Arm32, Abi.X86 -> api
        Abi.Arm64, Abi.X86_64 -> max(api, 21)
    }

    /**
     * An Android ABI.
     *
     * See https://developer.android.com/ndk/guides/abis for more information.
     *
     * @param[targetArchAbi] The Android ABI name for this ABI. This matches the
     * `APP_ABI` ndk-build variable or `ANDROID_ABI` CMake toolchain variable.
     */
    enum class Abi(val targetArchAbi: String) {
        /**
         * 32-bit Arm.
         */
        Arm32("armeabi-v7a"),

        /**
         * 64-bit Arm.
         */
        Arm64("arm64-v8a"),

        /**
         * 32-bit x86.
         */
        X86("x86"),

        /**
         * 64-bit x86.
         */
        X86_64("x86_64");

        companion object {
            /**
             * Constructs an [Abi] from the given string.
             *
             * @param[str] A string matching an Android [ABI][Abi] name.
             * @throws[IllegalArgumentException] No matching [Abi] was found.
             * @return The [Abi] matching [str] if a match was found.
             */
            fun fromString(str: String): Abi =
                values().find { it.targetArchAbi == str }
                    ?: throw IllegalArgumentException("Unknown ABI: $str")
        }
    }

    override fun canUse(requirement: PlatformDataInterface): Boolean {
        if (requirement !is Android) {
            return false
        }

        if (abi != requirement.abi) {
            return false
        }

        if (api < requirement.api) {
            return false
        }

        // TODO: STL checking.
        return true
    }

    override fun libraryFileFromDirectory(
        directory: Path,
        module: Module
    ): Path = findElfLibrary(directory, module.libraryNameForPlatform(this))

    /**
     * The [Android] factory object.
     */
    companion object : PlatformFactoryInterface {
        override val identifier: String = "android"

        override fun fromLibraryDirectory(
            directory: Path
        ): PlatformDataInterface {
            val metadata = Json.parse<AndroidAbiMetadata>(
                directory.toFile().resolve("abi.json").readText()
            )

            return Android(Abi.fromString(metadata.abi), metadata.api)
        }

        override fun fromCommandLineArgs(
            abi: String?,
            osVersion: String?
        ): Collection<PlatformDataInterface> {
            require(osVersion != null) {
                throw IllegalArgumentException(
                    "Android targets require an OS version"
                )
            }

            return if (abi != null) {
                listOf(Android(Abi.fromString(abi), osVersion.toInt()))
            } else {
                // If --abi wasn't provided, build for every ABI.
                Abi.values().map { Android(it, osVersion.toInt()) }
            }
        }
    }
}
