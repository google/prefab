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
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AndroidTest {
    @Test
    fun `ABIs must match`() {
        val arm32 = Android(Android.Abi.Arm32, 21, Android.Stl.CxxShared)
        val arm64 = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared)
        val arm32Lib = mockk<PrebuiltLibrary>()
        val arm64Lib = mockk<PrebuiltLibrary>()
        every { arm32Lib.platform } returns arm32
        every { arm32Lib.path } returns Paths.get("libfoo.so")
        every { arm64Lib.platform } returns arm64
        every { arm64Lib.path } returns Paths.get("libfoo.so")
        assertFalse(arm32.canUse(arm64Lib))
        assertFalse(arm64.canUse(arm32Lib))
        assertTrue(arm64.canUse(arm64Lib))
    }

    @Test
    fun `OS version constraint only allows equal or older dependencies`() {
        val old = Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared)
        val new = Android(Android.Abi.Arm32, 21, Android.Stl.CxxShared)
        val oldLib = mockk<PrebuiltLibrary>()
        val newLib = mockk<PrebuiltLibrary>()
        every { oldLib.platform } returns old
        every { oldLib.path } returns Paths.get("libfoo.so")
        every { newLib.platform } returns new
        every { newLib.path } returns Paths.get("libfoo.so")
        assertTrue(new.canUse(oldLib))
        assertFalse(old.canUse(newLib))
    }

    @Test
    fun `STL matching constraints are enforced`() {
        val cxxShared = Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared)
        val cxxStatic = Android(Android.Abi.Arm64, 21, Android.Stl.CxxStatic)
        val gnuShared = Android(Android.Abi.Arm64, 21, Android.Stl.GnustlShared)
        val none = Android(Android.Abi.Arm64, 21, Android.Stl.None)
        val system = Android(Android.Abi.Arm64, 21, Android.Stl.System)

        // A shared library using c++_shared.
        val cxxSharedSharedLib = mockk<PrebuiltLibrary>()
        every { cxxSharedSharedLib.platform } returns cxxShared
        every { cxxSharedSharedLib.path } returns Paths.get("libfoo.so")

        // A static library using c++_shared.
        val cxxSharedStaticLib = mockk<PrebuiltLibrary>()
        every { cxxSharedStaticLib.platform } returns cxxShared
        every { cxxSharedStaticLib.path } returns Paths.get("libfoo.a")

        // A static library using c++_shared.
        val cxxStaticSharedLib = mockk<PrebuiltLibrary>()
        every { cxxStaticSharedLib.platform } returns cxxStatic
        every { cxxStaticSharedLib.path } returns Paths.get("libfoo.so")

        // A static library using c++_static.
        val cxxStaticStaticLib = mockk<PrebuiltLibrary>()
        every { cxxStaticStaticLib.platform } returns cxxStatic
        every { cxxStaticStaticLib.path } returns Paths.get("libfoo.a")

        // A shared library using gnustl_shared.
        val gnuSharedSharedLib = mockk<PrebuiltLibrary>()
        every { gnuSharedSharedLib.platform } returns gnuShared
        every { gnuSharedSharedLib.path } returns Paths.get("libfoo.so")

        // A shared library using gnustl_static.
        val gnuSharedStaticLib = mockk<PrebuiltLibrary>()
        every { gnuSharedStaticLib.platform } returns gnuShared
        every { gnuSharedStaticLib.path } returns Paths.get("libfoo.a")

        // A library using no STL.
        val noneLib = mockk<PrebuiltLibrary>()
        every { noneLib.platform } returns none

        // A library using the system STL.
        val systemLib = mockk<PrebuiltLibrary>()
        every { systemLib.platform } returns system

        assertTrue(cxxShared.canUse(cxxSharedSharedLib))
        assertTrue(cxxShared.canUse(cxxSharedStaticLib))
        assertFalse(cxxShared.canUse(cxxStaticSharedLib))
        assertTrue(cxxShared.canUse(cxxStaticStaticLib))
        assertFalse(cxxShared.canUse(gnuSharedSharedLib))
        assertFalse(cxxShared.canUse(gnuSharedStaticLib))
        assertTrue(cxxShared.canUse(noneLib))
        assertTrue(cxxShared.canUse(systemLib))

        assertFalse(cxxStatic.canUse(cxxSharedSharedLib))
        assertTrue(cxxStatic.canUse(cxxSharedStaticLib))
        assertFalse(cxxStatic.canUse(cxxStaticSharedLib))
        assertTrue(cxxStatic.canUse(cxxStaticStaticLib))
        assertFalse(cxxStatic.canUse(gnuSharedSharedLib))
        assertFalse(cxxStatic.canUse(gnuSharedStaticLib))
        assertTrue(cxxStatic.canUse(noneLib))
        assertTrue(cxxStatic.canUse(systemLib))

        assertFalse(gnuShared.canUse(cxxSharedSharedLib))
        assertFalse(gnuShared.canUse(cxxSharedStaticLib))
        assertFalse(gnuShared.canUse(cxxStaticSharedLib))
        assertFalse(gnuShared.canUse(cxxStaticStaticLib))
        assertTrue(gnuShared.canUse(gnuSharedSharedLib))
        assertTrue(gnuShared.canUse(gnuSharedStaticLib))
        assertTrue(gnuShared.canUse(noneLib))
        assertTrue(gnuShared.canUse(systemLib))

        assertFalse(none.canUse(cxxSharedSharedLib))
        assertFalse(none.canUse(cxxSharedStaticLib))
        assertFalse(none.canUse(cxxStaticSharedLib))
        assertFalse(none.canUse(cxxStaticStaticLib))
        assertFalse(none.canUse(gnuSharedSharedLib))
        assertFalse(none.canUse(gnuSharedStaticLib))
        assertTrue(none.canUse(noneLib))
        assertTrue(none.canUse(systemLib))

        assertFalse(system.canUse(cxxSharedSharedLib))
        assertFalse(system.canUse(cxxSharedStaticLib))
        assertFalse(system.canUse(cxxStaticSharedLib))
        assertFalse(system.canUse(cxxStaticStaticLib))
        assertFalse(system.canUse(gnuSharedSharedLib))
        assertFalse(system.canUse(gnuSharedStaticLib))
        assertTrue(system.canUse(noneLib))
        assertTrue(system.canUse(systemLib))
    }

    @Test
    fun `OS version pulled up to 21 for LP64`() {
        assertEquals(
            16,
            Android(Android.Abi.Arm32, 16, Android.Stl.CxxShared).api
        )
        assertEquals(
            21,
            Android(Android.Abi.Arm64, 16, Android.Stl.CxxShared).api
        )
    }
}