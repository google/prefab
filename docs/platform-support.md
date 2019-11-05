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

At present, only Android and GNU/Linux targets are supported. [Patches] to
support additional targets are welcome. Unlike build system support, platform
support cannot be provided as a plugin and must be directly implemented in
Prefab. This is to avoid the fragmentation of packages that would occur if there
were multiple implementations of support for a given platform. For example, if
there were both an `ubuntu` and a `linux-ubuntu` plugin with libraries that are
compatible, their packages would not be mutually usable. Keeping platform
support centralized avoids this issue.

[Patches]: CONTRIBUTING.md

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
    "stl": {
        "name": "libc++",
        "type": "shared"
    }
}
```

At present, the following rules are used to select Android libraries based on
the requested configuration:

1. ABI must match exactly.
2. API for a dependency cannot be higher than `--os-version`.

TODO: Implement additional requirements. STL checks, NDK version ABI boundaries.

### GNU/Linux

GNU/Linux libraries use an arbitrary identifier in the library directory name
used to separate the libraries only. Actual identification of the libraries
contained is done iwth an `abi.json` metadata file within the library directory.
This metadata has the following format:

```json
{
    "arch": "amd64",
    "glibc_version": "2.28"
}
```

At present, the following rules are used to select GNU/Linux libraries based on
the requested configuration:

1. ABI must match exactly.
2. glibc version for a dependency cannot be higher than `--os-version`.

## Platform API

TODO: Link to javadoc once published.
