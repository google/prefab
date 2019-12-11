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

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.github.johnrengelman.shadow").version("5.2.0")
}
dependencies {
    implementation("com.github.ajalt:clikt:2.2.0")
    testImplementation("io.mockk:mockk:1.9.3")

    implementation(project(":api"))
    runtimeOnly(project(":cmake-plugin"))
    runtimeOnly(project(":ndk-build-plugin"))

    testImplementation(project(":cmake-plugin"))
    testImplementation(project(":ndk-build-plugin"))
}

application {
    // Define the main class for the application.
    mainClassName = "com.google.prefab.cli.AppKt"
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }

    named("build") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            configure<ShadowExtension> {
                component(this@create)
            }

            pom {
                name.set("Prefab")
                description.set("The main Prefab program.")
                url.set(rootProject.property("prefab.pom.url") as String)
                packaging = "jar"
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
