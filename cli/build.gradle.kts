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

plugins {
    application
}

extra["pomName"] = "Prefab"
extra["pomDescription"] = "The main Prefab program."

dependencies {
    implementation("com.github.ajalt:clikt:2.2.0")
    testImplementation("io.mockk:mockk:1.9.3")

    implementation(project(":api"))
    implementation(project(":cmake-plugin"))
    implementation(project(":ndk-build-plugin"))
}

application {
    // Define the main class for the application.
    mainClassName = "com.google.prefab.cli.AppKt"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClassName
    }
}