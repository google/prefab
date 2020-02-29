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

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidTest {
    @Test
    fun `ABIs must match`() {
        val arm32 = Android(Android.Abi.Arm32, 21, Android.Stl.CxxShared, 21)
        val arm64 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val arm32Lib = mockk<PrebuiltLibrary>()
        val arm64Lib = mockk<PrebuiltLibrary>()
        every { arm32Lib.platform } returns arm32
        every { arm32Lib.path } returns Paths.get("libfoo.so")
        every { arm64Lib.platform } returns arm64
        every { arm64Lib.path } returns Paths.get("libfoo.so")
        assertEquals(
            IncompatibleLibrary(
                "User is targeting armeabi-v7a but library is for arm64-v8a"
            ), arm32.checkIfUsable(arm64Lib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User is targeting arm64-v8a but library is for armeabi-v7a"
            ), arm64.checkIfUsable(arm32Lib)
        )
        assertTrue(arm64.checkIfUsable(arm64Lib) is CompatibleLibrary)
    }

    @Test
    fun `OS version constraint only allows equal or older dependencies`() {
        val old = Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared, 21)
        val new = Android(Android.Abi.Arm32, 21, Android.Stl.CxxShared, 21)
        val oldLib = mockk<PrebuiltLibrary>()
        val newLib = mockk<PrebuiltLibrary>()
        every { oldLib.platform } returns old
        every { oldLib.path } returns Paths.get("libfoo.so")
        every { newLib.platform } returns new
        every { newLib.path } returns Paths.get("libfoo.so")
        assertTrue(new.checkIfUsable(oldLib) is CompatibleLibrary)
        assertEquals(
            IncompatibleLibrary(
                "User has minSdkVersion 16 but library was built for 21"
            ), old.checkIfUsable(newLib)
        )
    }

    @Test
    fun `STL matching constraints are enforced`() {
        val cxxShared =
            Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val cxxStatic =
            Android(Android.Abi.Arm64, 21, Android.Stl.CxxStatic, 21)
        val gnuShared =
            Android(Android.Abi.Arm64, 21, Android.Stl.GnustlShared, 21)
        val none = Android(Android.Abi.Arm64, 21, Android.Stl.None, 21)
        val system = Android(Android.Abi.Arm64, 21, Android.Stl.System, 21)

        // A shared library using c++_shared.
        val cxxSharedSharedLib = mockk<PrebuiltLibrary>()
        every { cxxSharedSharedLib.platform } returns cxxShared
        every { cxxSharedSharedLib.path } returns Paths.get("libs/libfoo.so")

        // A static library using c++_shared.
        val cxxSharedStaticLib = mockk<PrebuiltLibrary>()
        every { cxxSharedStaticLib.platform } returns cxxShared
        every { cxxSharedStaticLib.path } returns Paths.get("libs/libfoo.a")

        // A shared library using c++_static.
        val cxxStaticSharedLib = mockk<PrebuiltLibrary>()
        every { cxxStaticSharedLib.platform } returns cxxStatic
        every { cxxStaticSharedLib.path } returns Paths.get("libs/libfoo.so")

        // A static library using c++_static.
        val cxxStaticStaticLib = mockk<PrebuiltLibrary>()
        every { cxxStaticStaticLib.platform } returns cxxStatic
        every { cxxStaticStaticLib.path } returns Paths.get("libs/libfoo.a")

        // A shared library using gnustl_shared.
        val gnuSharedSharedLib = mockk<PrebuiltLibrary>()
        every { gnuSharedSharedLib.platform } returns gnuShared
        every { gnuSharedSharedLib.path } returns Paths.get("libs/libfoo.so")

        // A shared library using gnustl_static.
        val gnuSharedStaticLib = mockk<PrebuiltLibrary>()
        every { gnuSharedStaticLib.platform } returns gnuShared
        every { gnuSharedStaticLib.path } returns Paths.get("libs/libfoo.a")

        // A library using no STL.
        val noneLib = mockk<PrebuiltLibrary>()
        every { noneLib.platform } returns none

        // A library using the system STL.
        val systemLib = mockk<PrebuiltLibrary>()
        every { systemLib.platform } returns system

        assertTrue(
            cxxShared.checkIfUsable(cxxSharedSharedLib) is CompatibleLibrary
        )
        assertTrue(
            cxxShared.checkIfUsable(cxxSharedStaticLib) is CompatibleLibrary
        )
        assertEquals(
            IncompatibleLibrary(
                "Library is a shared library with a statically linked STL " +
                        "and cannot be used with any library using the STL"
            ), cxxShared.checkIfUsable(cxxStaticSharedLib)
        )
        assertTrue(
            cxxShared.checkIfUsable(cxxStaticStaticLib) is CompatibleLibrary
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libc++ but library requires libstdc++"
            ), cxxShared.checkIfUsable(gnuSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libc++ but library requires libstdc++"
            ), cxxShared.checkIfUsable(gnuSharedStaticLib)
        )
        assertTrue(cxxShared.checkIfUsable(noneLib) is CompatibleLibrary)
        assertTrue(cxxShared.checkIfUsable(systemLib) is CompatibleLibrary)

        assertEquals(
            IncompatibleLibrary(
                "User is using a static STL but library requires a shared STL"
            ), cxxStatic.checkIfUsable(cxxSharedSharedLib)
        )
        assertTrue(
            cxxStatic.checkIfUsable(cxxSharedStaticLib) is CompatibleLibrary
        )
        assertEquals(
            IncompatibleLibrary(
                "Library is a shared library with a statically linked STL " +
                        "and cannot be used with any library using the STL"
            ), cxxStatic.checkIfUsable(cxxStaticSharedLib)
        )
        assertTrue(
            cxxStatic.checkIfUsable(cxxStaticStaticLib) is CompatibleLibrary
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libc++ but library requires libstdc++"
            ), cxxStatic.checkIfUsable(gnuSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libc++ but library requires libstdc++"
            ), cxxStatic.checkIfUsable(gnuSharedStaticLib)
        )
        assertTrue(cxxStatic.checkIfUsable(noneLib) is CompatibleLibrary)
        assertTrue(cxxStatic.checkIfUsable(systemLib) is CompatibleLibrary)

        assertEquals(
            IncompatibleLibrary(
                "User requested libstdc++ but library requires libc++"
            ), gnuShared.checkIfUsable(cxxSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libstdc++ but library requires libc++"
            ), gnuShared.checkIfUsable(cxxSharedStaticLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libstdc++ but library requires libc++"
            ), gnuShared.checkIfUsable(cxxStaticSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested libstdc++ but library requires libc++"
            ), gnuShared.checkIfUsable(cxxStaticStaticLib)
        )
        assertTrue(
            gnuShared.checkIfUsable(gnuSharedSharedLib) is CompatibleLibrary
        )
        assertTrue(
            gnuShared.checkIfUsable(gnuSharedStaticLib) is CompatibleLibrary
        )
        assertTrue(gnuShared.checkIfUsable(noneLib) is CompatibleLibrary)
        assertTrue(gnuShared.checkIfUsable(systemLib) is CompatibleLibrary)

        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), none.checkIfUsable(cxxSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), none.checkIfUsable(cxxSharedStaticLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), none.checkIfUsable(cxxStaticSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), none.checkIfUsable(cxxStaticStaticLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libstdc++"
            ), none.checkIfUsable(gnuSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libstdc++"
            ), none.checkIfUsable(gnuSharedStaticLib)
        )
        assertTrue(none.checkIfUsable(noneLib) is CompatibleLibrary)
        assertTrue(none.checkIfUsable(systemLib) is CompatibleLibrary)

        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), system.checkIfUsable(cxxSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), system.checkIfUsable(cxxSharedStaticLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), system.checkIfUsable(cxxStaticSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libc++"
            ), system.checkIfUsable(cxxStaticStaticLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libstdc++"
            ), system.checkIfUsable(gnuSharedSharedLib)
        )
        assertEquals(
            IncompatibleLibrary(
                "User requested no STL but library requires libstdc++"
            ), system.checkIfUsable(gnuSharedStaticLib)
        )
        assertTrue(system.checkIfUsable(noneLib) is CompatibleLibrary)
        assertTrue(system.checkIfUsable(systemLib) is CompatibleLibrary)
    }

    @Test
    fun `best match for API level is found`() {
        val lollipop = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val marshmallow =
            Android(Android.Abi.Arm64, 23, Android.Stl.CxxShared, 21)
        val nougat = Android(Android.Abi.Arm64, 24, Android.Stl.CxxShared, 21)
        val pie = Android(Android.Abi.Arm64, 28, Android.Stl.CxxShared, 21)
        val arm32 = Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared, 21)

        val module = mockk<Module>()
        every { module.canonicalName } returns "//foo/bar"

        val lollipopLib = mockk<PrebuiltLibrary>()
        every { lollipopLib.platform } returns lollipop
        every { lollipopLib.path } returns Paths.get("libfoo.so")
        every { lollipopLib.module } returns module

        val marshmallowLib = mockk<PrebuiltLibrary>()
        every { marshmallowLib.platform } returns marshmallow
        every { marshmallowLib.path } returns Paths.get("libfoo.so")
        every { marshmallowLib.module } returns module

        val pieLib = mockk<PrebuiltLibrary>()
        every { pieLib.platform } returns pie
        every { pieLib.path } returns Paths.get("libfoo.so")
        every { pieLib.module } returns module

        assertEquals(lollipopLib, lollipop.findBestMatch(listOf(lollipopLib)))
        assertEquals(
            marshmallowLib,
            marshmallow.findBestMatch(listOf(lollipopLib, marshmallowLib))
        )
        assertEquals(
            marshmallowLib,
            nougat.findBestMatch(listOf(lollipopLib, marshmallowLib))
        )
        assertEquals(
            pieLib,
            pie.findBestMatch(listOf(marshmallowLib, lollipopLib, pieLib))
        )

        assertThrows<IllegalArgumentException> {
            lollipop.findBestMatch(emptyList())
        }

        assertThrows<IllegalArgumentException> {
            arm32.findBestMatch(listOf(marshmallowLib))
        }
    }

    @Test
    fun `best match for NDK version is found`() {
        val r18 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18)
        val r19 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 19)
        val r20 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 20)
        val r21 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21)
        val r22 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 22)
        val arm32 = Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared, 21)

        val module = mockk<Module>()
        every { module.canonicalName } returns "//foo/bar"

        val r19Lib = mockk<PrebuiltLibrary>()
        every { r19Lib.platform } returns r19
        every { r19Lib.path } returns Paths.get("libfoo.so")
        every { r19Lib.module } returns module

        val r20Lib = mockk<PrebuiltLibrary>()
        every { r20Lib.platform } returns r20
        every { r20Lib.path } returns Paths.get("libfoo.so")
        every { r20Lib.module } returns module

        val r21Lib = mockk<PrebuiltLibrary>()
        every { r21Lib.platform } returns r21
        every { r21Lib.path } returns Paths.get("libfoo.so")
        every { r21Lib.module } returns module

        assertEquals(r19Lib, r18.findBestMatch(listOf(r19Lib, r20Lib, r21Lib)))
        assertEquals(r19Lib, r19.findBestMatch(listOf(r19Lib, r20Lib, r21Lib)))
        assertEquals(r20Lib, r20.findBestMatch(listOf(r19Lib, r20Lib, r21Lib)))
        assertEquals(r21Lib, r21.findBestMatch(listOf(r19Lib, r20Lib, r21Lib)))
        assertEquals(r21Lib, r22.findBestMatch(listOf(r19Lib, r20Lib, r21Lib)))

        assertThrows<IllegalArgumentException> {
            r19.findBestMatch(emptyList())
        }

        assertThrows<IllegalArgumentException> {
            arm32.findBestMatch(listOf(r19Lib))
        }

        assertThrows<RuntimeException> {
            r19.findBestMatch((listOf(r19Lib, r19Lib)))
        }

        assertThrows<RuntimeException> {
            r20.findBestMatch((listOf(r19Lib, r21Lib)))
        }
    }

    @Test
    fun `OS version pulled up to 21 for LP64`() {
        assertEquals(
            16,
            Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared, 21).api
        )
        assertEquals(
            21,
            Android(Android.Abi.Arm64, 16, Android.Stl.CxxShared, 21).api
        )
    }
}