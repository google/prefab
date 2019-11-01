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

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(kotlin("serialization:1.3.50"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
    }
}

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version("1.3.50")
    kotlin("plugin.serialization").version("1.3.50")
    distribution
    id("maven-publish")
    id("com.github.jk1.dependency-license-report").version("1.11")
    id("org.jetbrains.dokka").version("0.10.0")
}

repositories {
    jcenter()
}

subprojects {
    group = "com.google.prefab"
    version = "0.1.0"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "maven-publish")

    repositories {
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))

        // Use JUnit 5.
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0-M1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0-M1")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
        kotlinOptions.freeCompilerArgs += listOf(
            "-progressive",
            "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
        )
    }

    publishing {
        publications {
            create<MavenPublication>("default") {
                from(components["java"])
            }
        }

        repositories {
            maven {
                url = uri("${rootProject.buildDir}/repository")
            }
        }
    }
}

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
        subProjects = subprojects.map { it.name }
        configuration {
            reportUndocumented = true
        }
    }
}

licenseReport {
    allowedLicensesFile = projectDir.resolve("config/allowed_licenses.json")
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
