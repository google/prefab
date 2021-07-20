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
 * Base type for all Android abi.json versions.
 */
interface AndroidAbiMetadata : Metadata {
    /**
     * Migrates this object to the latest metadata version if necessary.
     *
     * If this object is already the latest version of the metadata, a copy is
     * returned.
     */
    fun migrate(module: Module, directory: Path): AndroidAbiMetadataV2

    companion object :
        MetadataLoader<AndroidAbiMetadata, AndroidAbiMetadataV2, Module> {
        override fun metadataClassFor(
            schemaVersion: SchemaVersion
        ): VersionedMetadataLoader<AndroidAbiMetadata> = when (schemaVersion) {
            SchemaVersion.V1 -> AndroidAbiMetadataV1
            SchemaVersion.V2 -> AndroidAbiMetadataV2
        }

        override fun loadAndMigrate(
            schemaVersion: SchemaVersion, directory: Path, data: Module
        ): AndroidAbiMetadataV2 {
            val diskMetadata = metadataClassFor(schemaVersion).load(directory)
            return diskMetadata.migrate(data, directory)
        }
    }
}
