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

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    testImplementation("io.mockk:mockk:1.13.9")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])

            pom {
                name.set("Prefab Plugin API")
                description.set("The API for Prefab plugins.")
                url.set(rootProject.property("prefab.pom.url") as String)
                licenses {
                    license {
                        name.set(
                            rootProject.property(
                                "prefab.pom.licenseName"
                            ) as String
                        )
                        url.set(
                            rootProject.property(
                                "prefab.pom.licenseUrl"
                            ) as String
                        )
                        distribution.set(
                            rootProject.property(
                                "prefab.pom.licenseDistribution"
                            ) as String
                        )
                    }
                }
            }
        }
    }
}