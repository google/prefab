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

/**
 * The version number of the repository.
 *
 * Used as the version number for snapshot (non-tagged) builds. Tagged builds
 * will be matched to this to prevent tagging releases with the wrong version.
 */
val baseVersion: String = "1.0.0"

/**
 * The name of the tag being built.
 *
 * This is passed from the cloud builder as described by
 * https://cloud.google.com/cloud-build/docs/configuring-builds/substitute-variable-values.
 *
 * If not defined, this is a snapshot build.
 */
val tagName: String? = rootProject.findProperty("prefab.tagName") as String?

/**
 * The qualifier portion of the version for this build.
 *
 * May be empty. If non-empty, the value will be hyphen-prefixed.
 *
 * For non-tagged builds, the qualifier is "-SNAPSHOT". For tagged builds the
 * qualifier depends on the tag, but will be either alpha, beta, milestone, or
 * rc, and will contain a numeric identifier. For example, "-alpha10" or "-rc1".
 *
 * If no qualifier is found in the tag, this is a final release build and the
 * qualifier will be the empty string.
 */
val qualifier: String = if (tagName != null) {
    val pattern =
        """^v(\d+\.\d+\.\d+)(?:-((?:alpha|beta|milestone|rc)\d+))?$""".toRegex()
    val match = pattern.find(tagName)
    require(match != null) {
        "prefab.tagName did not match expected tag pattern"
    }
    val versionMatch = match.groups[1]
    require(versionMatch != null)
    require(versionMatch.value == baseVersion) {
        "Expected tag version to be $baseVersion"
    }
    match.groups[2]?.let {
        "-${it.value}"
    } ?: ""
} else {
    "-SNAPSHOT"
}

subprojects {
    group = "com.google.prefab"
    version = "$baseVersion$qualifier"

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

    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("default") {
                    from(components["java"])

                    pom {
                        name.set(extra["pomName"] as String)
                        description.set(extra["pomDescription"] as String)
                        url.set("https://google.github.io/prefab/")
                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set(
                                    "http://www.apache.org/licenses/LICENSE-2.0.txt"
                                )
                                distribution.set("repo")
                            }
                        }
                        scm {
                            connection.set(
                                "scm:git:https://github.com/google/prefab.git"
                            )
                            developerConnection.set(
                                "scm:git:git@github.com:google/prefab.git"
                            )
                            url.set("https://github.com/google/prefab")
                        }
                        issueManagement {
                            system.set("GitHub")
                            url.set("https://github.com/google/prefab/issues")
                        }
                    }
                }
            }

            repositories {
                maven {
                    url = uri("${rootProject.buildDir}/repository")
                }
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

tasks.register("release") {
    dependsOn(":build")
    dependsOn(":dokka")
}
