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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path

/**
 * A minimal portion of the package metadata that is loaded to determine the
 * schema version of the package.
 *
 * Not handled as part of package metadata loading since we can't know which
 * package metadata format to load until we've determined the package's schema
 * version.
 *
 * @property[schemaVersion] The version of the schema.
 */
@Serializable
data class SchemaVersionMetadata(
    @SerialName("schema_version")
    val schemaVersion: Int,
)

/**
 * Enumeration of all known package schema versions.
 *
 * @property[version] The integer representation of the schema version.
 */
enum class SchemaVersion(val version: Int) {
    /**
     * The V1 package schema.
     *
     * This is the schema version that Prefab initially shipped with. It is
     * supported by all current versions of Prefab.
     */
    V1(1),

    /**
     * The V2 package schema.
     *
     * This is the schema version supported by Prefab 2 and newer. Changes were
     * made to:
     *
     * * Android's abi.json. See [AndroidAbiMetadataV2] for more details.
     */
    V2(2);

    companion object {
        /**
         * Returns the [SchemaVersion] matching the given version number.
         *
         * @param[version] The integer representation of the schema version.
         * @return The [SchemaVersion] matching [version].
         * @throws[IllegalArgumentException] No matching schema version was
         * found.
         */
        fun from(version: Int): SchemaVersion {
            try {
                return values().single { it.version == version }
            } catch (ex: NoSuchElementException) {
                throw IllegalArgumentException(
                    "schema_version must be between 1 and ${LATEST.version}. " +
                            "Package uses version ${version}.", ex)
            }
        }

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Returns the [SchemaVersion] set in the `schema_version` property of
         * the given package directory.
         *
         * @param[packageDirectory] Path to the package directory.
         * @return The schema version set in the package metadata file.
         */
        fun from(packageDirectory: Path): SchemaVersion = from(
            json.decodeFromString<SchemaVersionMetadata>(
                packageDirectory.resolve("prefab.json").toFile().readText()
            ).schemaVersion
        )

        /**
         * The latest [SchemaVersion] supported by this version of Prefab.
         */
        private val LATEST: SchemaVersion = V2
    }
}
