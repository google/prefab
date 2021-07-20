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
 * Base type for migratable metadata.
 */
interface Metadata

/**
 * Interface for objects that load and migrate metadata across schema versions.
 *
 * @param[BaseMetadata] The parent type for the type of metadata handled by the
 * loader.
 * @param[CurrentMetadata] The type of the latest metadata version.
 * @param[LoaderData] Additional data passed to loadAndMigrate to support
 * migration of old versions.
 */
interface MetadataLoader<
        BaseMetadata : Metadata,
        CurrentMetadata : BaseMetadata,
        LoaderData,
        > {
    /**
     * Returns the metadata type for the given schema version.
     *
     * @param[schemaVersion] The schema version of the package.
     */
    fun metadataClassFor(
        schemaVersion: SchemaVersion
    ): VersionedMetadataLoader<BaseMetadata>

    /**
     * Loads the metadata from disk and migrates it to the latest version if
     * necessary.
     *
     * @param[schemaVersion] The schema version of the package.
     * @param[directory] The directory containing the file to be loaded.
     * @param[data] The additional data needed to migrate old metadata.
     * @return The metadata that has been migrated to the latest version.
     */
    fun loadAndMigrate(
        schemaVersion: SchemaVersion, directory: Path, data: LoaderData
    ): CurrentMetadata
}

/**
 * Interface for objects that load metadata from disk without migration.
 *
 * @param[T] The metadata type handled by the loader.
 */
interface VersionedMetadataLoader<out T : Metadata> {
    /**
     * Loads metadata from the given directory.
     *
     * @param[directory] The directory containing the file to be loaded.
     * @return The metadata loaded from the given directory.
     */
    fun load(directory: Path): T
}
