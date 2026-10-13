# Application version

Set the release version in the root `gradle.properties`:

```properties
appVersion=1.0.0
appVersionCode=1
```

`appVersion` supplies the version displayed on the Impressum screen, Android's
`versionName`, and desktop's native distribution `packageVersion`. Use numeric
`major.minor.patch` versions that meet the target installer format's limits.
Prerelease suffixes are not supported.

`appVersionCode` is a positive integer used by Android. Increment it for each
published Android release; it is independent of `appVersion`.

Override the defaults for an individual build or in CI:

```bash
./gradlew :androidApp:assembleDebug -PappVersion=1.2.3 -PappVersionCode=12
./gradlew :desktopApp:packageDistributionForCurrentOS -PappVersion=1.2.3
```

The shared module's `generateAppBuildInfo` task generates the Kotlin version
constant under `shared/build/generated/kotlin/appBuildInfo`. Compilation runs
this task automatically, and changing `appVersion` regenerates the constant
without a clean build. Do not edit or commit the generated file.

The `version` string resources contain only the localized format (`Version %1$s`);
they do not need to be changed for releases.
