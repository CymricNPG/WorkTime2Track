# Repository Guidelines

## Project Structure & Module Organization

WorkTime2Track is a Kotlin Multiplatform/Compose project targeting Android and JVM desktop.

- `shared/src/commonMain/` contains shared domain, data, UI, dependency-injection, and Compose resource code.
- `shared/src/androidMain/` and `shared/src/jvmMain/` contain platform-specific implementations.
- `shared/src/commonTest/`, `shared/src/androidHostTest/`, and `shared/src/jvmTest/` contain shared and target-specific tests.
- `androidApp/` and `desktopApp/` are thin platform launchers and packaging modules.
- `shared/src/commonMain/sqldelight/` defines the SQLDelight database schema.
- `docs/` contains architecture notes, known errors, and phased implementation plans.

Keep reusable behavior in `shared`; add platform code only when an API or integration requires it.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper and JDK 17.

- `./gradlew build` — compile all modules and run the standard verification lifecycle.
- `./gradlew :androidApp:assembleDebug` — build the Android debug APK.
    - `./gradlew :shared:jvmTest` — run desktop/shared JVM tests.

## Coding Style & Naming Conventions

Follow Kotlin conventions: four-space indentation, trailing commas in multiline declarations, and explicit imports. Use `PascalCase` for types and composables, `camelCase` for functions and properties, and descriptive suffixes such as `Service`, `Repository`, `ViewModel`, and `Screen`. Keep packages under `net.npg.wt2t`.

Place user-facing text in Compose string resources rather than inline literals. Preserve `expect`/`actual` boundaries for platform behavior. No repository-wide formatter or linter is configured, so format changed files with the IDE before committing.

For detailed coding rules see [Rules-Kotlin.md](.aiassistant/rules/Rules-Kotlin.md)

## Testing Guidelines

Tests use `kotlin.test`, coroutine test utilities, and JUnit Platform. Name test classes after the subject (`TimeServiceTest`) and use behavior-focused test names. Add common tests for shared logic and target-specific tests only for platform implementations. Run both shared test commands before opening a pull request.

## Commit & Pull Request Guidelines

Recent history mostly uses terse lowercase `wip` commits; avoid extending that pattern. Prefer concise imperative messages such as `Add project validation` or `Fix desktop report export`.

Pull requests should explain the behavior change, list affected targets, link relevant issues or plan documents, and report test commands run. Include screenshots for Compose UI changes and call out database schema or configuration changes explicitly.

## Agent Responsibilities

Lead developers own shared architecture and consistency. Mobile and desktop specialists own platform integrations and layouts. QA changes should cover shared behavior and both targets where applicable.

- When in doubt, ask the user instead of making assumptions.
- If a Gradle build fails because of locked files or build artifacts, ask the user to stop any parallel Gradle build before retrying.

## Requirements, Concept, Design

The purpose of the application is described in German in [Konzept.md](docs/Konzept.md)
