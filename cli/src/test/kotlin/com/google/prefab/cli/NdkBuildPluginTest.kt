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

package com.google.prefab.cli

import com.google.prefab.api.Android
import com.google.prefab.api.Package
import com.google.prefab.api.SchemaVersion
import com.google.prefab.ndkbuild.DuplicateModuleNameException
import com.google.prefab.ndkbuild.NdkBuildPlugin
import com.google.prefab.ndkbuild.sanitize
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class NdkBuildPluginTest(override val schemaVersion: SchemaVersion) :
    PerSchemaTest {
    companion object {
        @Parameterized.Parameters(name = "schema version = {0}")
        @JvmStatic
        fun data(): List<SchemaVersion> = SchemaVersion.values().toList()
    }

    private val staleOutputDir: Path =
        Files.createTempDirectory("stale").apply {
        toFile().apply { deleteOnExit() }
    }

    private val staleFile: Path = staleOutputDir.resolve("bogus").apply {
        toFile().createNewFile()
    }

    private val outputDirectory: Path = Files.createTempDirectory("output").apply {
        toFile().apply { deleteOnExit() }
    }

    @Test
    fun `stale files are removed from extant output directory`() {
        assertTrue(staleFile.toFile().exists())
        NdkBuildPlugin(staleOutputDir.toFile(), emptyList()).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 21))
        )
        assertFalse(staleFile.toFile().exists())
    }

    @Test
    fun `basic project generates correctly`() {
        val fooPath = packagePath("foo")
        val foo = Package(fooPath)

        val quxPath = packagePath("qux")
        val qux = Package(quxPath)

        NdkBuildPlugin(outputDirectory.toFile(), listOf(foo, qux)).generate(
            Android.Abi.values().map {
                Android(
                    it,
                    19,
                    Android.Stl.CxxShared,
                    21
                )
            }
        )

        val fooAndroidMk = outputDirectory.resolve("foo/Android.mk").toFile()
        assertTrue(fooAndroidMk.exists())

        val barDir = fooPath.resolve("modules/bar").sanitize()
        val bazDir = fooPath.resolve("modules/baz").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.armeabi-v7a/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.armeabi-v7a/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # armeabi-v7a

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.arm64-v8a/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.arm64-v8a/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # arm64-v8a

            ifeq ($(TARGET_ARCH_ABI),riscv64)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.riscv64/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.riscv64/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # riscv64

            ifeq ($(TARGET_ARCH_ABI),x86)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.x86/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.x86/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # x86

            ifeq ($(TARGET_ARCH_ABI),x86_64)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.x86_64/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.x86_64/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # x86_64

            $(call import-module,prefab/quux)
            $(call import-module,prefab/qux)

            """.trimIndent(), fooAndroidMk.readText()
        )

        val quxAndroidMk = outputDirectory.resolve("qux/Android.mk").toFile()
        assertTrue(quxAndroidMk.exists())

        val quxDir = quxPath.resolve("modules/libqux").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.armeabi-v7a/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include ${'$'}(PREBUILT_STATIC_LIBRARY)

            endif  # armeabi-v7a

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.arm64-v8a/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include ${'$'}(PREBUILT_STATIC_LIBRARY)

            endif  # arm64-v8a

            ifeq ($(TARGET_ARCH_ABI),riscv64)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.riscv64/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include ${'$'}(PREBUILT_STATIC_LIBRARY)

            endif  # riscv64

            ifeq ($(TARGET_ARCH_ABI),x86)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.x86/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include ${'$'}(PREBUILT_STATIC_LIBRARY)

            endif  # x86

            ifeq ($(TARGET_ARCH_ABI),x86_64)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.x86_64/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include ${'$'}(PREBUILT_STATIC_LIBRARY)

            endif  # x86_64

            $(call import-module,prefab/foo)

            """.trimIndent(), quxAndroidMk.readText()
        )
    }

    @Test
    fun `singe ABI project generates correctly`() {
        val fooPath = packagePath("foo")
        val foo = Package(fooPath)

        val quxPath = packagePath("qux")
        val qux = Package(quxPath)

        NdkBuildPlugin(outputDirectory.toFile(), listOf(foo, qux)).generate(
            listOf(Android(Android.Abi.Arm64, 19, Android.Stl.CxxShared, 21))
        )

        val fooAndroidMk = outputDirectory.resolve("foo/Android.mk").toFile()
        assertTrue(fooAndroidMk.exists())

        val quxAndroidMk = outputDirectory.resolve("qux/Android.mk").toFile()
        assertTrue(quxAndroidMk.exists())

        val barDir = fooPath.resolve("modules/bar").sanitize()
        val bazDir = fooPath.resolve("modules/baz").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.arm64-v8a/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS := -landroid
            include $(PREBUILT_SHARED_LIBRARY)

            include ${'$'}(CLEAR_VARS)
            LOCAL_MODULE := baz
            LOCAL_SRC_FILES := $bazDir/libs/android.arm64-v8a/libbaz.so
            LOCAL_EXPORT_C_INCLUDES := $bazDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES := libqux
            LOCAL_EXPORT_LDLIBS := -llog
            include ${'$'}(PREBUILT_SHARED_LIBRARY)

            endif  # arm64-v8a

            $(call import-module,prefab/quux)
            $(call import-module,prefab/qux)

            """.trimIndent(), fooAndroidMk.readText()
        )

        val quxDir = quxPath.resolve("modules/libqux").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := libqux
            LOCAL_SRC_FILES := $quxDir/libs/android.arm64-v8a/libqux.a
            LOCAL_EXPORT_C_INCLUDES := $quxDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES := bar
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_STATIC_LIBRARY)

            endif  # arm64-v8a

            $(call import-module,prefab/foo)

            """.trimIndent(), quxAndroidMk.readText()
        )
    }

    @Test
    fun `duplicate module names raise an error`() {
        val moduleA = Package(packagePath("duplicate_module_names_a"))
        val moduleB = Package(packagePath("duplicate_module_names_b"))

        assertFailsWith<DuplicateModuleNameException> {
            NdkBuildPlugin(
                outputDirectory.toFile(),
                listOf(moduleA, moduleB)
            ).generate(
                listOf(
                    Android(
                        Android.Abi.Arm64,
                        19,
                        Android.Stl.CxxShared,
                        21
                    )
                )
            )
        }
    }

    @Test
    fun `header only module works`() {
        val packagePath = packagePath("header_only")
        val pkg = Package(packagePath)
        NdkBuildPlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 19, Android.Stl.CxxShared, 21))
        )

        val androidMk =
            outputDirectory.resolve("header_only/Android.mk").toFile()
        assertTrue(androidMk.exists())

        val fooDir = packagePath.resolve("modules/foo").sanitize()
        val barDir = packagePath.resolve("modules/bar").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := bar
            LOCAL_SRC_FILES := $barDir/libs/android.arm64-v8a/libbar.so
            LOCAL_EXPORT_C_INCLUDES := $barDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES := foo
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := foo
            LOCAL_EXPORT_C_INCLUDES := $fooDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(BUILD_STATIC_LIBRARY)

            endif  # arm64-v8a


            """.trimIndent(), androidMk.readText()
        )
    }

    @Test
    fun `per-platform includes work`() {
        val path = packagePath("per_platform_includes")
        val pkg = Package(path)

        NdkBuildPlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            Android.Abi.values().map {
                Android(
                    it,
                    19,
                    Android.Stl.CxxShared,
                    21
                )
            }
        )

        val androidMk =
            outputDirectory.resolve("per_platform_includes/Android.mk").toFile()
        assertTrue(androidMk.exists())

        // Only some of the platforms in this module have their own headers, so
        // some of these module definitions point into the platform-specific
        // directory while others point to the module's headers.
        val modDir = path.resolve("modules/perplatform").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := perplatform
            LOCAL_SRC_FILES := $modDir/libs/android.armeabi-v7a/libperplatform.so
            LOCAL_EXPORT_C_INCLUDES := $modDir/libs/android.armeabi-v7a/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # armeabi-v7a

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := perplatform
            LOCAL_SRC_FILES := $modDir/libs/android.arm64-v8a/libperplatform.so
            LOCAL_EXPORT_C_INCLUDES := $modDir/libs/android.arm64-v8a/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # arm64-v8a

            ifeq ($(TARGET_ARCH_ABI),riscv64)

            include $(CLEAR_VARS)
            LOCAL_MODULE := perplatform
            LOCAL_SRC_FILES := $modDir/libs/android.riscv64/libperplatform.so
            LOCAL_EXPORT_C_INCLUDES := $modDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # riscv64

            ifeq ($(TARGET_ARCH_ABI),x86)

            include $(CLEAR_VARS)
            LOCAL_MODULE := perplatform
            LOCAL_SRC_FILES := $modDir/libs/android.x86/libperplatform.so
            LOCAL_EXPORT_C_INCLUDES := $modDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # x86

            ifeq ($(TARGET_ARCH_ABI),x86_64)

            include $(CLEAR_VARS)
            LOCAL_MODULE := perplatform
            LOCAL_SRC_FILES := $modDir/libs/android.x86_64/libperplatform.so
            LOCAL_EXPORT_C_INCLUDES := $modDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            endif  # x86_64


            """.trimIndent(), androidMk.readText()
        )
    }

    @Test
    fun `mixed static and shared libraries are both exposed when compatible`() {
        val packagePath =  packagePath("static_and_shared")
        val pkg = Package(packagePath)
        NdkBuildPlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxShared, 18))
        )

        val androidMk =
            outputDirectory.resolve("static_and_shared/Android.mk").toFile()
        assertTrue(androidMk.exists())

        val fooDir = packagePath.resolve("modules/foo").sanitize()
        val fooStaticDir = packagePath.resolve("modules/foo_static").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := foo
            LOCAL_SRC_FILES := $fooDir/libs/android.shared/libfoo.so
            LOCAL_EXPORT_C_INCLUDES := $fooDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_SHARED_LIBRARY)

            include $(CLEAR_VARS)
            LOCAL_MODULE := foo_static
            LOCAL_SRC_FILES := $fooStaticDir/libs/android.static/libfoo.a
            LOCAL_EXPORT_C_INCLUDES := $fooStaticDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_STATIC_LIBRARY)

            endif  # arm64-v8a


            """.trimIndent(), androidMk.readText()
        )
    }

    @Test
    fun `incompatible libraries are skipped for static STL`() {
        val packagePath =  packagePath("static_and_shared")
        val pkg = Package(packagePath)
        NdkBuildPlugin(outputDirectory.toFile(), listOf(pkg)).generate(
            listOf(Android(Android.Abi.Arm64, 21, Android.Stl.CxxStatic, 18))
        )

        val androidMk =
            outputDirectory.resolve("static_and_shared/Android.mk").toFile()
        assertTrue(androidMk.exists())

        val fooStaticDir = packagePath.resolve("modules/foo_static").sanitize()
        assertEquals(
            """
            LOCAL_PATH := $(call my-dir)

            ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

            include $(CLEAR_VARS)
            LOCAL_MODULE := foo_static
            LOCAL_SRC_FILES := $fooStaticDir/libs/android.static/libfoo.a
            LOCAL_EXPORT_C_INCLUDES := $fooStaticDir/include
            LOCAL_EXPORT_SHARED_LIBRARIES :=
            LOCAL_EXPORT_STATIC_LIBRARIES :=
            LOCAL_EXPORT_LDLIBS :=
            include $(PREBUILT_STATIC_LIBRARY)

            endif  # arm64-v8a


            """.trimIndent(), androidMk.readText()
        )
    }
}
