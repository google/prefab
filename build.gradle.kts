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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Project Kotlin version.
 */
val kotlinVersion: String = "1.5.20"

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version("1.5.20")
    kotlin("plugin.serialization").version("1.5.20")
    distribution
    id("maven-publish")
    id("com.github.jk1.dependency-license-report").version("1.16")
    id("org.jetbrains.dokka").version("1.4.32")
}

repositories {
    mavenCentral()
}

subprojects {
    val versionBase = rootProject.property("prefab.version") as String
    require(versionBase.matches("""^\d+\.\d+\.\d+$""".toRegex())) {
        "prefab.version is not in major.minor.path format"
    }
    val qualifier = rootProject.property("prefab.qualifier") as String
    require(qualifier.matches("""^(-(alpha|beta|rc)\d+)?$""".toRegex())) {
        "prefab.qualifier did not match the expected format"
    }
    group = "com.google.prefab"
    version = versionBase + if (!rootProject.hasProperty("prefab.release")) {
        "-SNAPSHOT"
    } else {
        qualifier
    }

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))

        // Use JUnit 5.
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    }

    publishing {
        repositories {
            maven {
                url = uri("${rootProject.buildDir}/repository")
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
    }

    java {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    outputDirectory.set(buildDir.resolve("dokka"))
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(true)
        }
    }
}

licenseReport {
    allowedLicensesFile = projectDir.resolve("config/allowed_licenses.json")
    excludes = listOf(
        // These dependencies don't have direct license information but inherit
        // that data from a parent project, which the license checking plugin
        // can't follow, so it would fail because it cannot find a license.
        // Each license is explicitly listed below and checking is skipped.

        // Apache 2.0
        "com.github.ajalt.clikt:clikt",
        "org.jetbrains.kotlinx:kotlinx-serialization-core",
        "org.jetbrains.kotlinx:kotlinx-serialization-json",
    ).toTypedArray()
}

tasks.named("check") {
    dependsOn(":checkLicense")
}

distributions {
    create("repository") {
        contents {
            from(buildDir.resolve("repository"))
        }
    }
}

tasks.named("distTar") {
    dependsOn(":repositoryDistTar")
}

tasks.named("distZip") {
    dependsOn(":repositoryDistZip")
}

tasks.named("repositoryDistTar") {
    subprojects.map { dependsOn(":${it.name}:publish") }
}

tasks.named("repositoryDistZip") {
    subprojects.map { dependsOn(":${it.name}:publish") }
}

tasks.register("release") {
    dependsOn(":build")
    dependsOn(":dokkaHtmlMultiModule")
}
