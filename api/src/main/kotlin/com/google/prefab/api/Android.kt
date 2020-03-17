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

import kotlinx.serialization.UnstableDefault
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
 * @property[ndkMajorVersion] The major version of the Android NDK targeted by
 * this build.
 * @constructor Creates an Android requirements object.
 */
class Android(val abi: Abi, api: Int, val stl: Stl, val ndkMajorVersion: Int) :
    PlatformDataInterface {
    override val targetTriple: String = abi.triple

    val api: Int = when (abi) {
        Abi.Arm32, Abi.X86 -> api
        Abi.Arm64, Abi.X86_64 -> max(api, 21)
    }

    override fun toString(): String = "Android($abi, $api, $stl)"

    /**
     * An Android ABI.
     *
     * See https://developer.android.com/ndk/guides/abis for more information.
     *
     * @property[targetArchAbi] The Android ABI name for this ABI. This matches
     * the `APP_ABI` ndk-build variable or `ANDROID_ABI` CMake toolchain
     * variable.
     * @property[triple] The target triple associated with this ABI. Note that
     * this is the library architecture rather than the exact target, so 32-bit
     * Arm is arm-linux-androideabi rather than armv7a-linux-androideabi.
     */
    enum class Abi(val targetArchAbi: String, val triple: String) {
        /**
         * 32-bit Arm.
         */
        Arm32("armeabi-v7a", "arm-linux-androideabi"),

        /**
         * 64-bit Arm.
         */
        Arm64("arm64-v8a", "aarch64-linux-android"),

        /**
         * 32-bit x86.
         */
        X86("x86", "i686-linux-android"),

        /**
         * 64-bit x86.
         */
        X86_64("x86_64", "x86_64-linux-android");

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
        enum class Family(val familyName: String) {
            /**
             * libc++.
             */
            Cxx("libc++"),

            /**
             * GNU libstdc++.
             */
            Gnustl("libstdc++"),

            /**
             * None or system. STLs with no linking restrictions.
             */
            None("no STL"),

            /**
             * STLport.
             */
            Stlport("STLport")
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

    private fun stlsAreCompatible(library: PrebuiltLibrary):
            LibraryUsabilityResult {
        require(library.platform is Android) {
            "library must be an Android library"
        }

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
            return CompatibleLibrary
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
            return IncompatibleLibrary(
                "User requested ${stl.family.familyName} but library " +
                        "requires ${library.platform.stl.family.familyName}"
            )
        }

        val pathMatcher = library.path.fileSystem.getPathMatcher("glob:*.a")
        if (pathMatcher.matches(library.path.fileName)) {
            // The dependency is a static library, so its choice of static or
            // shared STL is not actually meaningful; it'll use whatever the
            // user uses. No further checking required.
            return CompatibleLibrary
        }

        if (!library.platform.stl.isShared) {
            // The dependency is a shared library that has statically linked
            // the STL. This prevents the user or any of the user's dependencies
            // from using the STL. We can't perform that check, so this library
            // can't be reliably used anywhere.
            return IncompatibleLibrary(
                "Library is a shared library with a statically linked STL " +
                        "and cannot be used with any library using the STL"
            )
        }

        if (!stl.isShared) {
            // For the same reason, the user can't use a static STL if their
            // dependencies are using a shared STL.
            return IncompatibleLibrary(
                "User is using a static STL but library requires a shared STL"
            )
        }

        // Our STLs are of the same family, our dependency is a shared
        // library that is using a shared STL, and we're using a shared STL.
        return CompatibleLibrary
    }

    override fun checkIfUsable(library: PrebuiltLibrary): LibraryUsabilityResult {
        if (library.platform !is Android) {
            return IncompatibleLibrary("Library is not an Android library")
        }

        if (abi != library.platform.abi) {
            return IncompatibleLibrary(
                "User is targeting ${abi.targetArchAbi} but library is for " +
                        library.platform.abi.targetArchAbi
            )
        }

        if (api < library.platform.api) {
            return IncompatibleLibrary(
                "User has minSdkVersion $api but library was built for " +
                        library.platform.api
            )
        }

        return stlsAreCompatible(library)

        // TODO: Check for ABI boundaries across NDK major versions.
        // At present the only known ABI break in the NDK was when the libc++
        // ABI changed in r11. No one should actually use libc++ in either of
        // those releases, nor should they be using those releases any more, so
        // this isn't an urgent check to add. If we introduce more ABI breaks in
        // the future we'll want to make sure we add checks for those.
    }

    override fun findBestMatch(
        libraries: List<PrebuiltLibrary>
    ): PrebuiltLibrary {
        require(libraries.isNotEmpty()) { "libraries must be non-empty" }
        require(libraries.all { checkIfUsable(it) is CompatibleLibrary }) {
            "all libraries must be compatible"
        }
        val moduleName = libraries.first().module.canonicalName

        val allLibraries: List<Pair<PrebuiltLibrary, Android>> = libraries.map {
            require(it.platform is Android) {
                "library must be an android library"
            }
            Pair(it, it.platform)
        }

        // Filter out any libraries that were built with a lower API level.
        // Libraries built for newer API levels may expose additional features
        // the rely on system APIs not available in earlier API levels, and may
        // be smaller since libandroid_support is only required on very old API
        // levels.
        val bestApiLevel = allLibraries.maxBy { it.second.api }!!.second.api
        val bestApiLevelMatches = allLibraries.filter { (_, reqs) ->
            reqs.api == bestApiLevel
        }

        if (bestApiLevelMatches.size == 1) {
            // If a single match was found, return it as a valid match even if
            // it is not a matching NDK version. This is probably the common
            // case.
            return bestApiLevelMatches.single().first
        }

        // If we still have multiple matches, perform NDK version matching.
        // If the user's NDK version is outside the span supported by the
        // module, clamp it to the supported range. Return an exact match for
        // the clamped version.
        assert(bestApiLevelMatches.isNotEmpty())
        val minNdkVersion = bestApiLevelMatches.minBy {
            it.second.ndkMajorVersion
        }!!.second.ndkMajorVersion
        val maxNdkVersion = bestApiLevelMatches.maxBy {
            it.second.ndkMajorVersion
        }!!.second.ndkMajorVersion
        val clamped =
            ndkMajorVersion.coerceIn(minNdkVersion, maxNdkVersion)
        val ndkVersionMatches = bestApiLevelMatches.filter { (_, reqs) ->
            reqs.ndkMajorVersion == clamped
        }

        // Catch any gaps in NDK versions supported by the library.
        if (ndkVersionMatches.isEmpty()) {
            throw RuntimeException(
                "$moduleName contains a library per NDK version but no match " +
                        "was found for $ndkMajorVersion")
        }

        if (ndkVersionMatches.size == 1) {
            return ndkVersionMatches.single().first
        }

        // TODO: Add load-time validation for modules.
        // Only reporting these errors when a user encounters them makes it
        // easier to ship these issues. If the module is rejected immediately
        // the author will be able to stop the issue before they ship it.
        //
        // There may be cases where the library author has done something like
        // have both a c++_static and c++_shared variant of a static library.
        // There's no need to have both in this case.
        throw RuntimeException(
            "Unable to resolve a single library match for $moduleName. The " +
                    "following libraries are redundant:\n" +
                    ndkVersionMatches.joinToString(separator = "\n") {
                        it.first.directory.toString()
                    }
        )
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

        @OptIn(UnstableDefault::class)
        override fun fromLibraryDirectory(
            directory: Path
        ): PlatformDataInterface {
            val metadata = Json.parse<AndroidAbiMetadata>(
                directory.toFile().resolve("abi.json").readText()
            )

            return Android(
                Abi.fromString(metadata.abi),
                metadata.api,
                Stl.fromString(metadata.stl),
                metadata.ndk
            )
        }
    }
}
