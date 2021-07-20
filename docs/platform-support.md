# Platform Support in Prefab

Prefab is designed to support arbitrary target platforms.

## Packaging for Multiple Platforms

The package author can choose to distribute their packages as either a fat
archive which contains libraries for each target they support or a package per
target. Fat packages are the simplest to use because the user does not need to
consider which package to import, but for packages that support a large number
of platforms these packages could be quite large.

There is no required method for splitting a package by target. A target-specific
package is simply a differently named package that contains only a subset of the
prebuilt libraries.

For example, to split a fat archive "foo" which contains libraries for four
Android ABIs, two versions of Ubuntu, and Windows, the package would likely be
split into the following packages:

* `foo-android`
* `foo-ubuntu-focal`
* `foo-ubuntu-xenial`
* `foo-windows`

## Supported Platforms

At present, only Android targets are supported. Support for desktop Linux is a
[work in progress], with some outstanding questions about exactly what
constraints need to be defined for that platform. [Patches] to support
additional targets are welcome. Unlike build system support, platform support
cannot be provided as a plugin and must be directly implemented in Prefab. This
is to avoid the fragmentation of packages that would occur if there were
multiple implementations of support for a given platform. For example, if there
were both an `ubuntu` and a `linux-ubuntu` plugin with libraries that are
compatible, their packages would not be mutually usable. Keeping platform
support centralized avoids this issue.

[Patches]: https://github.com/google/prefab/blob/master/CONTRIBUTING.md
[work in progress]: https://github.com/google/prefab/pull/90

### Android

Android libraries use an arbitrary identifier in the library directory name used
to separate the libraries only. Actual identification of the libraries contained
is done with an `abi.json` metadata file within the library directory. This
metadata has the following format:

```json
{
    "abi": "arm64-v8a",
    "api": 24,
    "ndk": 19,
    "stl": "libc++_shared",
    "static": true
}
```

The `static` property is optional and defaults to `false`. This property is used
to determine whether the library in the directory is a static or shared library,
and therefore the file extension of the library. In the V1 schema this property
was determined based on the contents of the directory, but in Prefab 2 the libraries are not required to exist before generating build scripts.

At present, the following rules are used to select Android libraries based on
the requested configuration:

1. ABI must match exactly.
2. API for a dependency cannot be higher than `--os-version`.
3. STLs must be compatible as defined by
   `com.google.prefab.api.Android::stlsAreCompatible`.

## Platform API

TODO: Link to javadoc once published.
