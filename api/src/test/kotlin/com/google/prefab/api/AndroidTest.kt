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
        every { arm64Lib.platform } returns arm64
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
        every { newLib.platform } returns new
        assertTrue(new.canUse(oldLib))
        assertFalse(old.canUse(newLib))
    }

    @Test
    fun `STL matching constraints are enforced`() {
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