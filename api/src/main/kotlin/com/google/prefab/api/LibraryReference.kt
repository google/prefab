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

/**
 * A reference to a library exported from a [Module].
 *
 * Library references may refer to an arbitrary library that is expected to be
 * available in the sysroot, a library within the current package, or a library
 * within a different package that this package depends on.
 */
sealed class LibraryReference {
    /**
     * A link flag to be used as-is. e.g. "-lfoo".
     *
     * @property[arg] The literal library argument to be exported.
     */
    data class Literal(val arg: String) : LibraryReference() {
        companion object {
            /**
             * Verifies that a the given reference is a valid [Literal]
             * reference.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             */
            private fun validate(reference: String) {
                require(reference.isNotEmpty()) {
                    "Literal library reference must not be empty"
                }
            }

            /**
             * Creates a [LibraryReference.Literal] from the given string.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             * @return A [LibraryReference.Literal] matching [reference].
             */
            fun fromString(reference: String): Literal {
                validate(reference)
                return Literal(reference)
            }
        }
    }

    /**
     * A reference to another module within the same package. e.g. ":foo".
     *
     * @property[name] The name of the module to be exported.
     */
    data class Local(val name: String) : LibraryReference() {
        companion object {
            /**
             * Verifies that a the given reference is a valid [Local] reference.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             */
            private fun validate(reference: String) {
                assert(reference.startsWith(":"))
                require(reference.count { it == ':' } == 1) {
                    "Expected exactly one : in local library reference"
                }
            }


            /**
             * Creates a [LibraryReference.Local] from the given string.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             * @return A [LibraryReference.Local] matching [reference].
             */
            fun fromString(reference: String): Local {
                validate(reference)
                return Local(reference.substring(1))
            }
        }
    }

    /**
     * A reference to a module within another package. e.g. "//bar:baz".
     *
     * @property[pkg] The package the exported module belongs to.
     * @property[module] The name of the exported module.
      */
    data class External(
        val pkg: String,
        val module: String
    ) : LibraryReference() {
        companion object {
            /**
             * Verifies that a the given reference is a valid [External]
             * reference.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             */
            private fun validate(reference: String) {
                assert(reference.startsWith("//"))

                require(reference.count { it == ':' } == 1) {
                    "Expected exactly one : in external library reference"
                }

                require(!reference.substring(2).contains('/')) {
                    "Expected no / after leading // in external library " +
                            "reference"
                }
            }

            /**
             * Creates a [LibraryReference.External] from the given string.
             *
             * @param[reference] The string representation of the
             * [LibraryReference].
             * @return A [LibraryReference.External] matching [reference].
             */
            fun fromString(reference: String): External {
                validate(reference)
                val (pkg, module) = reference.substring(2).split(":")
                return External(pkg, module)
            }
        }
    }

    companion object {
        /**
         * Constructs the appropriate type of [LibraryReference] from the given
         * string.
         *
         * @param[reference] The string representation of the [LibraryReference]
         * as found in the [module metadata][ModuleMetadataV1].
         * @return A [LibraryReference] matching [reference].
         */
        fun fromString(reference: String): LibraryReference = when {
            reference.startsWith("//") -> External.fromString(reference)
            reference.startsWith(":") -> Local.fromString(reference)
            else -> Literal.fromString(reference)
        }
    }
}
