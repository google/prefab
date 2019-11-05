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

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GnuLinuxTest {
    @Test
    fun `verify glibc version ordering`() {
        assertTrue(GnuLinux.GlibcVersion(2, 28) > GnuLinux.GlibcVersion(2, 14))
        assertTrue(GnuLinux.GlibcVersion(2, 28) >= GnuLinux.GlibcVersion(2, 14))
        assertTrue(GnuLinux.GlibcVersion(2, 14) < GnuLinux.GlibcVersion(2, 28))
        assertTrue(GnuLinux.GlibcVersion(2, 14) <= GnuLinux.GlibcVersion(2, 28))
        assertTrue(GnuLinux.GlibcVersion(2, 28) >= GnuLinux.GlibcVersion(2, 28))
        assertTrue(GnuLinux.GlibcVersion(2, 28) <= GnuLinux.GlibcVersion(2, 28))
        assertEquals(GnuLinux.GlibcVersion(2, 28), GnuLinux.GlibcVersion(2, 28))
    }

    @Test
    fun `glibc versions are parsed correctly`() {
        assertEquals(
            GnuLinux.GlibcVersion(2, 14),
            GnuLinux.GlibcVersion.fromString("2.14")
        )

        assertEquals(
            GnuLinux.GlibcVersion(2, 28),
            GnuLinux.GlibcVersion.fromString("2.28")
        )
    }

    @Test
    fun `glibc versions require exactly two version components`() {
        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString(".28")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2.")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2.28-10")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2.28.10")
        }
    }

    @Test
    fun `glibc versions may not contain whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2.28 ")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString(" 2.28")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("2 . 28")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("\t2.28")
        }

        assertFailsWith<IllegalArgumentException> {
            GnuLinux.GlibcVersion.fromString("")
        }
    }

    @Test
    fun `Architectures must match`() {
        val amd64 = GnuLinux(GnuLinux.Arch.Amd64, GnuLinux.GlibcVersion(2, 28))
        val ppc = GnuLinux(GnuLinux.Arch.Ppc64el, GnuLinux.GlibcVersion(2, 28))
        assertFalse(amd64.canUse(ppc))
        assertFalse(ppc.canUse(amd64))
        assertTrue(amd64.canUse(amd64))
    }

    @Test
    fun `OS version constraint only allows equal or older dependencies`() {
        val old = GnuLinux(GnuLinux.Arch.Amd64, GnuLinux.GlibcVersion(2, 14))
        val new = GnuLinux(GnuLinux.Arch.Amd64, GnuLinux.GlibcVersion(2, 28))
        assertTrue(new.canUse(old))
        assertFalse(old.canUse(new))
    }
}