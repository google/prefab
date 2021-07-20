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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageTest {
    @Test
    fun `isValidVersionForCMake works`() {
        assertTrue(isValidVersionForCMake("1"))
        assertTrue(isValidVersionForCMake("1.2"))
        assertTrue(isValidVersionForCMake("1.2.3"))
        assertTrue(isValidVersionForCMake("1.2.3.4"))

        assertFalse(isValidVersionForCMake(""))
        assertFalse(isValidVersionForCMake("."))
        assertFalse(isValidVersionForCMake("1."))
        assertFalse(isValidVersionForCMake(".1"))
        assertFalse(isValidVersionForCMake(" 1 "))
        assertFalse(isValidVersionForCMake("1.2."))
        assertFalse(isValidVersionForCMake("1.2.3."))
        assertFalse(isValidVersionForCMake("1.2.3.4."))
        assertFalse(isValidVersionForCMake("1.2.3.4.5"))
        assertFalse(isValidVersionForCMake("a"))
        assertFalse(isValidVersionForCMake("1.a"))
        assertFalse(isValidVersionForCMake("1.2.a"))
        assertFalse(isValidVersionForCMake("1.2.3.a"))
        assertFalse(isValidVersionForCMake("1a"))
        assertFalse(isValidVersionForCMake("1.2a"))
        assertFalse(isValidVersionForCMake("1.2.3a"))
        assertFalse(isValidVersionForCMake("1.2.3.4a"))
    }
}
