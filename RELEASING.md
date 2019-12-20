# Release Workflow

Version information is maintained in the `gradle.properties` file with the
`prefab.version` and `prefab.qualifier` properties. The version number should be
in the `<major>.<minor>.<patch>` format and the qualifier is either empty (for a
final release) or should be in the `-<RELEASE_TYPE><INCREMENT>` format. The
release type should be alpha, beta, or rc, and the increment begins at 1 and
increases for each additional release of the same version and type. For example,
the second release candidate of 1.0.0 should have the following entries in
gradle.properties:

```properties
prefab.version = 1.0.0
prefab.qualifier = -rc2
```

Version 1.0.0 final would have the following:

```properties
prefab.version = 1.0.0
prefab.qualifier =
```

## Creating a release build

The concatenated `<VERSION_NUMBER><QUALIFIER>` will be referred to as
`<VERSION>` for the remainder of this doc.

To create a new release of Prefab:

1. Verify that CHANGELOG.md is up to date. Release notes should exist for each
   release version, but not for each alpha/beta/rc. For example, 1.0.0-rc1's
   release notes should go in the 1.0.0 section.
2. Checkout and update the master branch, create a branch to prepare the
   release.

   ```bash
   git checkout master
   git pull
   git checkout -b release-<VERSION>
   ```

   If releasing from a point other than the HEAD of master, create the branch
   from that point instead.

3. Update the version number and qualifier in gradle.properties.
4. Test and commit changes:

   ```bash
   ./gradlew -Pprefab.release clean release
   git commit -a -m 'Release Prefab v<VERSION>.'
   ```

5. Tag the release:

   ```bash
   git tag -a v<VERSION> -m 'Prefab release version <VERSION>.'
   ```

6. Push the tag:

   ```bash
   git push origin --tags
   ```

   The build server will now automatically begin building the release from the
   tag.

## Publishing the release to Maven

Publishing the artifacts to Maven can only be done by a Googler with the
permissions to do so. That part of the process is documented at
http://go/prefab-release-process.

## Updating master after a stable release

After shipping a stable (non-qualified) release, the version number in master
needs to be updated. The next version should be `<VERSION>` with the patch
component incremented by one. This version number will be referred to as
`<NEXT_VERSION>` for the remainder of this doc.

1. Checkout and update the master branch, create a branch to prepare the pull
   request.

   ```bash
   git checkout master
   git pull
   git checkout -b update-version-<NEXT_VERSION>
   ```

2. Update the gradle.properties file with the new version number.
3. Create a new empty section for the new version in CHANGELOG.md.
4. Test and commit changes:

   ```bash
   ./gradlew clean build
   git commit -a -m 'Bump version to <NEXT_VERSION>.'
   ```

5. Send the pull request for the commit, get it reviewed, submit the pull
   request to master.
