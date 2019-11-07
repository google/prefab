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
 * @property[stl] The Android STL targeted by this build.
 * @constructor Creates an Android requirements object.
 */
class Android(val abi: Abi, api: Int, val stl: Stl) : PlatformDataInterface {
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

    /**
     * An Android STL.
     *
     * See https://developer.android.com/ndk/guides/cpp-support for more
     * information. Note that we include support for STLs not listed on that
     * page because we do support old artifacts that may have been built with a
     * now unsupported NDK.
     *
     * @property[stlName] The name of the STL.
     * @property[family] The family of the STL.
     * @property[isShared] True if this STL is a shared library.
     */
    enum class Stl(val stlName: String, val family: Family, val isShared: Boolean) {
        /**
         * Shared libc++.
         */
        CxxShared("c++_shared", Family.Cxx, true),

        /**
         * Static libc++.
         */
        CxxStatic("c++_static", Family.Cxx, false),

        /**
         * Shared GNU libstdc++.
         */
        GnustlShared("gnustl_shared", Family.Gnustl, true),

        /**
         * Static GNU libstdc++.
         */
        GnustlStatic("gnustl_static", Family.Gnustl, false),

        /**
         * No STL used.
         */
        None("none", Family.None, false),

        /**
         * Shared STLport.
         */
        StlportShared("stlport_shared", Family.Stlport, true),

        /**
         * Static STLport.
         */
        StlportStatic("stlport_static", Family.Stlport, false),

        /**
         * Bionic's libstdc++.
         *
         * This is in the same family as [Stl.None] since it has the same
         * linking requirements.
         */
        System("system", Family.None, true);

        /**
         * The family of the STL.
         */
        enum class Family {
            /**
             * libc++.
             */
            Cxx,

            /**
             * GNU libstdc++.
             */
            Gnustl,

            /**
             * None or system. STLs with no linking restrictions.
             */
            None,

            /**
             * STLport.
             */
            Stlport
        }

        companion object {
            /**
             * Constructs an [Stl] from the given string.
             *
             * @param[str] A string matching an Android [STL][Stl].
             * @throws[IllegalArgumentException] No matching [Stl] was found.
             * @return The [Stl] matching [str] if a match was found.
             */
            fun fromString(str: String): Stl =
                values().find { it.stlName == str }
                    ?: throw IllegalArgumentException("Unknown STL: $str")
        }
    }

    private fun stlsAreCompatible(library: PrebuiltLibrary): Boolean {
        require(library.platform is Android)

        // The case not explicitly handled here is the case where the user is
        // statically linking an STL, but it is not exposed in their ABI and
        // they've limited their symbol visibility with a version script and are
        // careful to keep allocations and deallocations coming from the same
        // side of the ABI boundary. This is an exceptional case that we can't
        // detect well, so in that circumstance the user should specify their
        // STL as "none" even though they are using an STL.

        // No restrictions on libraries that don't use any STL, and the system
        // "STL" has similar requirements since it's not really an STL.
        if (library.platform.stl.family == Stl.Family.None) {
            return true
        }

        // Otherwise STLs must be the same family.
        // One quirk of this check is that we'll wrongly reject dependencies
        // that use the STL if the user is using none or system. The alternative
        // is that we accept any STL in that case, but then we'll wrongly allow
        // a mix of dependencies using different STLs.
        //
        // If the user's dependencies are using the STL, they must choose an STL
        // for their application even if their own libraries do not use one.
        if (stl.family != library.platform.stl.family) {
            return false
        }

        val pathMatcher = library.path.fileSystem.getPathMatcher("glob:*.a")
        if (pathMatcher.matches(library.path)) {
            // The dependency is a static library, so its choice of static or
            // shared STL is not actually meaningful; it'll use whatever the
            // user uses. No further checking required.
            return true
        }

        if (!library.platform.stl.isShared) {
            // The dependency is a shared library that has statically linked
            // the STL. This prevents the user or any of the user's dependencies
            // from using the STL. We can't perform that check, so this library
            // can't be reliably used anywhere.
            return false
        }

        if (!stl.isShared) {
            // For the same reason, the user can't use a static STL if their
            // dependencies are using a shared STL.
            return false
        }

        // Our STLs are of the same family, our dependency is a shared
        // library that is using a shared STL, and we're using a shared STL.
        return true
    }

    // TODO: Return a reason for library rejection.
    // If no matching library can be found in the module then we should emit an
    // error showing all the libraries we considered and why they were rejected.
    override fun canUse(library: PrebuiltLibrary): Boolean {
        if (library.platform !is Android) {
            return false
        }

        if (abi != library.platform.abi) {
            return false
        }

        if (api < library.platform.api) {
            return false
        }

        if (!stlsAreCompatible(library)) {
            return false
        }

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

            return Android(
                Abi.fromString(metadata.abi),
                metadata.api,
                Stl.fromString(metadata.stl)
            )
        }
    }
}
