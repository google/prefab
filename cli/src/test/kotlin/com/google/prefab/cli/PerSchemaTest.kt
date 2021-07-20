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

package com.google.prefab.cli

import com.google.prefab.api.SchemaVersion
import java.nio.file.Path
import java.nio.file.Paths

interface PerSchemaTest {
    val schemaVersion: SchemaVersion

    fun packagePath(name: String): Path = Paths.get(
        this.javaClass.getResource("packages/v${schemaVersion.version}/$name")
            .toURI()
    )
}