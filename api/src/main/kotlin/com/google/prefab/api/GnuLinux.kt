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

/**
 * Platform requirements for GNU/Linux.
 *
 * @property[arch] The architecture targeted by this build.
 * @property[glibcVersion] The glibc version targeted by this build.
 */
class GnuLinux(val arch: Arch, val glibcVersion: GlibcVersion) :
    PlatformDataInterface {
    /**
     * A GNU C library version.
     *
     * @property[major] The major version of the GNU C library.
     * @property[minor] The minor version of the GNU C library.
     */
    data class GlibcVersion(val major: Int, val minor: Int) :
        Comparable<GlibcVersion> {
        override fun compareTo(other: GlibcVersion): Int =
            compareValuesBy(this, other, { it.major }, { it.minor })

        companion object {
            /**
             * Constructs a [GlibcVersion] from the given string.
             *
             * @param[str] The string containing a major.minor version.
             * @throws[IllegalArgumentException] The string was not in the
             * expected format.
             * @return A [GlibcVersion] matching [str].
             */
            fun fromString(str: String): GlibcVersion {
                require(str.count { it == '.' } == 1) {
                    "Expected exactly one . in glibc version string."
                }

                val (major, minor) = str.split(".")
                return GlibcVersion(major.toInt(), minor.toInt())
            }
        }
    }

    /**
     * A GNU/Linux architecture.
     *
     * The list was taken from Ubuntu's current list of supported architectures,
     * https://help.ubuntu.com/lts/installation-guide/i386/ch02s01.html.
     *
     * @property[archName] The name of the architecture.
     */
    enum class Arch(val archName: String) {
        /**
         * 64-bit x86.
         */
        Amd64("amd64"),

        /**
         * 64-bit Arm.
         */
        @Suppress("unused")
        Arm64("arm64"),

        /**
         * 32-bit Arm, hard float ABI.
         */
        @Suppress("unused")
        Armhf("armhf"),

        /**
         * 32-bit x86.
         */
        @Suppress("unused")
        I386("i386"),

        /**
         * 64-bit little endian POWER8.
         */
        @Suppress("unused")
        Ppc64el("ppc64el");

        companion object {
            /**
             * Constructs an [Arch] from the given string.
             *
             * @param[str] The string matching an [Arch] name.
             * @throws[IllegalArgumentException] No matching [Arch] was found.
             * @return An [Arch] matching [str] if one was found.
             */
            fun fromString(str: String): Arch =
                values().find { it.archName == str }
                    ?: throw IllegalArgumentException(
                        "Unknown architecture: $str"
                    )
        }
    }

    override fun canUse(requirement: PlatformDataInterface): Boolean {
        if (requirement !is GnuLinux) {
            return false
        }

        if (arch != requirement.arch) {
            return false
        }

        if (glibcVersion < requirement.glibcVersion) {
            return false
        }

        return true
    }

    override fun libraryFileFromDirectory(
        directory: Path,
        module: Module
    ): Path = findElfLibrary(directory, module.libraryNameForPlatform(this))

    /**
     * The [GnuLinux] factory object.
     */
    companion object : PlatformFactoryInterface {
        override val identifier: String = "gnulinux"

        override fun fromLibraryDirectory(
            directory: Path
        ): PlatformDataInterface {
            val metadata = Json.parse<GnuLinuxAbiMetadata>(
                directory.toFile().resolve("abi.json").readText()
            )

            return GnuLinux(
                Arch.fromString(metadata.arch),
                GlibcVersion.fromString(metadata.glibcVersion)
            )
        }

        override fun fromCommandLineArgs(
            abi: String?,
            osVersion: String?
        ): Collection<PlatformDataInterface> {
            // We could instead generate for every ABI, or perhaps the ABI of
            // the current system.
            require(abi != null) { "GNU/Linux targets require an ABI" }

            // TODO: Default to host's glibc version for glibc systems?
            require(osVersion != null) {
                "GNU/Linux targets require an OS version"
            }

            return listOf(
                GnuLinux(
                    Arch.fromString(abi),
                    GlibcVersion.fromString(osVersion)
                )
            )
        }
    }
}
