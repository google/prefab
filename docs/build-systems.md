# Build System Support in Prefab

Prefab's primary task is generating the build system integrations needed to
consume the modules a package describes. This support is provided by plugins for
each build system. By default, Prefab includes and loads the plugins for [CMake]
and [ndk-build]. Additional plugins can be provided at run-time with the
`--plugin` option. For common build systems, please consider [contributing] your
plugin to Prefab.

[CMake]: https://cmake.org/
[contributing]: CONTRIBUTING.md
[ndk-build]: https://developer.android.com/ndk/guides/ndk-build

## Supported Build Systems

### CMake

When using CMake, a [config file] for each package that can be imported with
[`find_package`]. If the out directory is `/out/prefab` and your project depends
on curl, the consuming CMakeLists.txt should include the following:

```cmake
list(APPEND CMAKE_FIND_ROOT_PATH /out/prefab)
find_package(curl REQUIRED)

add_library(app SHARED app.cpp)
target_link_libraries(app curl::curl)
```

Note: If using the Android Gradle Plugin, the Prefab directory will be
automatically added to the search path, so you can omit the changes to
`CMAKE_FIND_ROOT_PATH`.

[config file package]: https://cmake.org/cmake/help/latest/manual/cmake-packages.7.html
[`find_package`]: https://cmake.org/cmake/help/latest/command/find_package.html

### ndk-build

When using ndk-build, each package will result in a subdirectory of the out
directory which contains an Android.mk describing each module. If the out
directory is `/out/prefab` and your project depends on curl, the consuming
Android.mk should include the following:

```makefile
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := app
# Additional module configuration...

# Links the curl library from the imported module and makes its headers
# available.
LOCAL_SHARED_LIBRARIES := curl

include $(BUILD_SHARED_LIBRARY)

# Add the prefab modules to the import path.
$(call import-add-path,/out)

# Import curl so we can depend on it.
$(call import-module,prefab/curl)
```

Note: If using the Android Gradle Plugin, the Prefab directory will be
automatically added to the search path, so you can omit the `import-add-path`
command.

## Plugin API

TODO: Link to javadoc once published.
