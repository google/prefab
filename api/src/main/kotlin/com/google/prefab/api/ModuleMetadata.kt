/*
 * Copyright 2021 Google LLC
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

import java.nio.file.Path

/**
 * Base type for all module.json versions.
 */
interface ModuleMetadata : Metadata {
    companion object :
        MetadataLoader<ModuleMetadata, ModuleMetadataV1, Unit> {
        override fun metadataClassFor(
            schemaVersion: SchemaVersion
        ): ModuleMetadataV1.Companion = when (schemaVersion) {
            SchemaVersion.V1 -> ModuleMetadataV1
            SchemaVersion.V2 -> ModuleMetadataV1
        }

        override fun loadAndMigrate(
            schemaVersion: SchemaVersion, directory: Path, data: Unit
        ): ModuleMetadataV1 =
            metadataClassFor(schemaVersion).load(directory)

        /**
         * Loads the metadata from disk and migrates it to the latest version if
         * necessary.
         *
         * @param[schemaVersion] The schema version of the package.
         * @param[directory] The directory containing the file to be loaded.
         * @return The metadata that has been migrated to the latest version.
         */
        fun loadAndMigrate(
            schemaVersion: SchemaVersion, directory: Path
        ): ModuleMetadataV1 = loadAndMigrate(schemaVersion, directory, Unit)
    }
}
