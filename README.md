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
[Build system agnostic]: docs/build-systems.md
[Cross-platform]: docs/platform-support.md
[metadata]: docs/index.md#metadata
