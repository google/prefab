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

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryReferenceTest {
    @Test
    fun `fromString handles literal references`() {
        assertEquals(
            LibraryReference.Literal("-lfoo"),
            LibraryReference.fromString("-lfoo")
        )
        assertEquals(
            LibraryReference.Literal("foo"),
            LibraryReference.fromString("foo")
        )
    }

    @Test
    fun `fromString handles local references`() {
        assertEquals(
            LibraryReference.Local("foo"),
            LibraryReference.fromString(":foo")
        )
        assertThrows<IllegalArgumentException> {
            LibraryReference.fromString(":foo:bar")
        }
        assertThrows<IllegalArgumentException> {
            LibraryReference.fromString("::foo")
        }
    }

    @Test
    fun `fromString handles external references`() {
        assertEquals(
            LibraryReference.External("foo", "bar"),
            LibraryReference.fromString("//foo:bar")
        )
        assertThrows<IllegalArgumentException> {
            LibraryReference.fromString("///foo::bar")
        }
        assertThrows<IllegalArgumentException> {
            LibraryReference.fromString("//foo:ba:r")
        }
        assertThrows<IllegalArgumentException> {
            LibraryReference.fromString("//foo:/bar")
        }
    }
}
