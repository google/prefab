# Example non-Android Gradle workflow

This page describes how to use Prefab packages for an Android build that does
not rely on the Android Gradle Plugin (AGP). This is only an example workflow.
The recommended workflow for Android is to use AGP.

## Prerequisites

- Java 8+
- [The Prefab CLI]. Download the "jar" artifact of the latest version.
- [CMake]
- [The Android NDK]
- [The ndkports googletest AAR]. Download the "aar" artifact of the latest
  version.

[The Prefab CLI]: https://maven.google.com/web/index.html#com.google.prefab:cli
[CMake]: https://cmake.org/download/
[The Android NDK]: https://developer.android.com/ndk/downloads
[The ndkports googletest AAR]: https://maven.google.com/web/index.html#com.android.ndk.thirdparty:googletest

## Running Prefab

The Prefab CLI can be run with `java -jar`. For example:

```bash
$ java -jar cli-2.0.0-all.jar --help
Usage: prefab [OPTIONS] PACKAGE_PATH...

  https://google.github.io/prefab/

Android specific configuration options:
  --abi TEXT                       Target ABI.
  --os-version TEXT                Target OS version.
  --stl [c++_shared|c++_static|gnustl_shared|gnustl_static|none|stlport_shared|stlport_static|system]
                                   STL used by the application.
  --ndk-version INT                Major version of the NDK used by the
                                   application.

Options:
  --build-system TEXT   Generate integration for the given build system.
  --output PATH         Output path for generated build system integration.
  --platform [android]  Target platform. Only 'android' is currently
                        supported.
  -h, --help            Show this message and exit
```

## Using Prefab to generate CMake packages

Android AARs that expose native libraries include a Prefab package in the
`prefab/` subdirectory of the AAR. Prefab operates on the Prefab package
directory and not on the AAR, since AAR packaging is not required. AARs are zip
files, so to extract the contents for prefab, run the following (on Linux or
Mac; for Windows just extract using explorer).

```bash
$ unzip googletest-1.11.0-beta-1.aar -d googletest
```

`googletest/prefab` now contains the metadata, libraries, and headers that
Prefab will use to generate CMake packages for your build. CMake handles only a
single build configuration at a time, so Prefab must be run once for each build
configuration. The generate packages for the arm64-v8a build of an app that uses
NDK r23, the shared libc++, and has a minSdkVersion of 21, run:

```bash
$ java -jar cli-2.0.0-all.jar  --platform android --abi arm64-v8a \
    --os-version 21 --stl c++_shared --ndk-version 23 --build-system cmake \
    --output buildscripts googletest/prefab
```

The `buildscripts` directory now contains the CMake package definitions:

```bash
$ find buildscripts -type f
buildscripts/lib/aarch64-linux-android/cmake/googletest/googletestConfigVersion.cmake
buildscripts/lib/aarch64-linux-android/cmake/googletest/googletestConfig.cmake
```

If you have multiple Prefab packages, pass each package directory to the same
`prefab` command and it will generate CMake packages for each.

To expose these packages to CMake, use `CMAKE_FIND_ROOT_PATH`. For example:

```bash
$ cmake \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DCMAKE_FIND_ROOT_PATH=buildscripts \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DANDROID_STL=c++_shared \
    $PATH_TO_CMAKE_PROJECT_SRC
```

CMake will search the `buildscripts` directory for packages when using
`find_package`.

```cmake
find_package(googletest REQUIRED CONFIG)

add_library(app_test SHARED app_test.cpp)
target_link_libraries(app_test googletest::googletest)
```
