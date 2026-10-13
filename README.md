# WorkTime2Track

WorkTime2Track is a local time-tracking application for a single user on Android and desktop. It lets users organize
work into projects and tasks, record and adjust time entries, add notes, set daily target hours, and review worked time
and overtime by day, project, or task. The application is designed for quick task switching, supports free-time entries
that do not count as working time, and can generate reports for selected periods.

This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications. It contains
  several subfolders:
    - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name. For
      example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls. Similarly, if you want
      to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
      folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`

### SonarQube analysis

Create a local `sonar.local.properties` file containing `sonar.token=<your-token>`. This file is ignored by Git and must never be committed.

- Run tests and generate the Kover XML coverage report: `./gradlew :shared:koverXmlReport`
- Compile the applications, generate coverage, and upload the analysis: `./gradlew sonar`

The analysis is published to the `WorkTime2Track` project at `http://gondor:9000`. This connection uses unencrypted HTTP and should only be used on a trusted network.

### Architecture

- [Architecture decisions and compliance review](docs/architecture-decisions.md)

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Generate SBOM

- Application builds generate target-specific CycloneDX JSON SBOMs under the corresponding module's
  `build/generated/composeResources/sbom/` directory and package them as `files/sbom.json`.
- `./gradlew cyclonedxBom` generates an additional aggregate analysis report at `build/reports/cyclonedx/bom.json`; it
  is not packaged into applications.
- Desktop release artifacts must be built on each supported host so their bundled SBOM reflects that host's Compose
  Desktop runtime. Android artifacts bundle the `releaseRuntimeClasspath` SBOM and can be built on any supported host.
- Generated SBOM serial numbers and timestamps are intentionally retained for artifact provenance. Generated files
  remain under ignored `build/` directories and are not committed.

## License and AI-assisted development

This project was developed with substantial assistance from generative AI.

To the extent that the project’s contributors hold copyright in their contributions, those contributions are licensed
under the MIT License. See [LICENSE](LICENSE).

Third-party code and dependencies remain subject to their respective licenses. No claim of exclusive copyright is made
over material that is not eligible for copyright protection.
