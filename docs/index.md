# Prefab

Prefab is a tool for generating build system integrations for prebuilt C/C++
libraries. A Prefab package consists of a small amount of [metadata] and the
prebuilt libraries it describes. Prefab is:

* [Build system agnostic]. Build system support is provided via a plugin API, so
  it's simple to provide support for any build system not supported out of the
  box.

* [Cross-platform] capable. Prefab currently handles only Android libraries, but
  is designed to handle any number of platforms.

* Distribution agnostic. Prefab is only an archive format, and can be
  distributed with whatever package management infrastructure best fits your use
  case. Android packages will commonly be distributed within an [AAR] via Maven
  to be easily used in an Android Gradle project, but distribution as a tarball
  or a git submodule would work just as well.

[AAR]: https://developer.android.com/studio/projects/android-library
[Build system agnostic]: build-systems.md
[Cross-platform]: platform-support.md
[metadata]: #metadata

## Usage

Note: The Android Gradle Plugin natively supports Prefab packages. See TODO for
more information.

Prefab is a command line tool the operates on the packages described in this
document. At least one package path must be given, and each package path should
point to the directory structure described in [Package
Structure](#package-structure).

TODO: Improve usage message.

```text
Usage: prefab [OPTIONS] PACKAGE_PATH...

  prefab

Android specific configuration options:
  --abi TEXT                       Target ABI.
  --os-version TEXT                Target OS version.
  --stl [c++_shared|c++_static|gnustl_shared|gnustl_static|none|stlport_shared|stlport_static|system]
                                   STL used by the application.

Options:
  --build-system TEXT  Generate integration for the given build system.
  --output DIRECTORY   Output path for generated build system integration.
  --plugin-path FILE   Path to build system integration plugin.
  --platform TEXT      Target platform. Only 'android' is currently supported.
  -h, --help           Show this message and exit
```

TODO: Example.

## Authoring

TODO: Write authoring guide.

## Package Structure {#package-structure}

A complete Prefab artifact is called a "Package", and each Package contains at
least one Module. A Module is a distinct library to be consumed by the
dependent project. Each Module has its own headers and at most one library per
target platform.

For example, the OpenSSL package contains both libssl and libcrypto modules. The
curl package contains just libcurl.

Note: Header-only libraries can be exposed by a Module with an empty `libs`
directory.

A Package has the following directory layout:

```text
<package name>/
    package.json
    modules/
        <module name>/
            module.json
            include/
            libs/
                <platform>.<id>/
                    include/
                    <lib>
                ...
        ...
```

package.json is the [package level metadata](#package-metadata) and module.json
is the [module level metadata](#module-metadata). These are required.

The per-module include directory is optional. If present, the directory will be
automatically added to the consumer's header search path.

The libs directory contains a subdirectory for every supported platform. The
`<platform>` portion of the directory name identifies the family of the platform
(for example, "android", "linux", or "windows") and more exact matching is
performed in a platform-dependent manner. For example, Android libraries use an
arbitrary identifier and read additional platform data such as ABI and minimum
supported OS version from an abi.json file contained in the directory.

The libs directory is optional. A module without any libraries is a header only
library and has no platform restrictions.

The file name of the contained library is determined in a platform-dependent
manner. For Android, the format is `lib${name}.so`, but for Windows it would be
`${name}.dll`.

The include directory within each platform's library directory is optional. If
present, that include path will be used instead of the module headers for that
platform.

TODO: Example.

## Metadata {#metadata}

Both Packages and Modules contain metadata to describe their use.

### Package Metadata {#package-metadata}

Package metadata describes the version of Prefab that the package was built for
and any inter-package dependencies the package may have.

```json
{
    "schema_version": 1,
    "name": "<package name>",
    "dependencies": [
        "<dependency name>",
        ...
    ]
}
```

The `schema_version` describes the generation of the package format. Any
incompatible change to either metadata format or the package layout will result
in an increase in the schema version. Whenever possible, old package versions
will continue to be supported when new schema versions are introduced.

`name` identifies the Package and should match the name of the containing
directory.

`dependencies` is a list of strings naming other Packages (as specified by each
Package's `name`) that this Package depends on. All dependent packages must be
available to Prefab during build script generation.

TODO: Example.

### Module Metadata {#module-metadata}

Module metadata describes requirements for the module's use. At present, the
only requirements that may be exported to dependents are additional libraries
that the dependent must link along with the module, but in the future this may
be extended to include required compiler flags such as a minimum C++ version.

```json
{
    "export_libraries": [
        "<library link specifier>",
    ],
    "library_name": "<library file name without file extension>",
    "android": {
      "export_libraries": [
          "<library link specifier>",
      ],
      "library_name": "<library file name without file extension>"
    }
}
```

`export_libraries` may specify either literal arguments to be used as-is,
intra-package references, or inter-package references.

| Value       | Result                                                       |
| ----------- | ------------------------------------------------------------ |
| `-lfoo`     | Consumers will use the `-lfoo` flag as-is when linking.      |
| `:foo`      | Consumers will link the `foo` module from this package.      |
| `//bar:baz` | Consumers will link the `baz` module from the `bar` package. |

TODO: Verify.
Note that, except for the first case, the headers for the modules will also be
made available to the consuming package.

`module_name` specifies the name of the library file without the file extension.
This field is optional. If not specified, the library name is assumed to be
`lib<module name>`.

The `android` and field allows either `export_libraries` or `library_name` to be
overridden dependening on the target platform. Each subfield follows the same
rules of the main properties with the same name. If specified, the
platform-specific properties override the generic properties.

TODO: Example.
