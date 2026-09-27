# Storage-Service Migration

## Why this migration exists

`StorageService` aggregates unrelated repositories and mostly forwards their methods. That lets view models and tests choose among several persistence paths, obscures workflow ownership, and makes transaction-sensitive operations and duplicate overview calculations easy to scatter.

The target architecture is:

```text
Presentation -> feature use case -> repository contract -> SQLDelight implementation -> SQLite
```

Presentation may use immutable shared models, but must not access repository, storage, or generated database abstractions. Feature use cases own business workflows and depend on only the repository contracts they require.

## Current state

- Production UI and Koin wiring use `BookingUseCase`, `ConfigurationUseCase`, `DayEditingUseCase`, `EndOfDayUseCase`, `ProjectManagementUseCase`, `ReportingUseCase`, and `BackupUseCase`.
- The Android navigation test, booking-transition repository test, day-deletion repository test, and integration-test fixture have begun migration.
- Legacy services remain only because tests still compile against them; they must be deleted after all callers migrate.
- Focused Gradle verification was started after test migrations, but no final result was captured. Run the commands below before deleting legacy code.

## Remaining work

1. [x] Migrate the remaining legacy tests:
   - `DatabaseExportRepositoryTest`
   - `EndOfDayServiceTest` -> `EndOfDayUseCaseTest`
   - `ProjectTaskServiceTest` -> `ProjectManagementUseCaseTest`
   - `StorageServiceTest` -> delete
   - `TimeServiceTest` -> `BookingUseCaseTest`
   - `UpdateDayUseCaseTest` -> `DayEditingUseCaseTest`
   - `BookingViewModelTest`
   - `ConfigViewModelTest`
   - `DailyOverviewViewModelTest`
   - `EditDayDeletionViewModelTest`
   - `EditDayViewModelTest`
   - `EndOfDayViewModelTest`

2. [x] Complete the reporting migration:
   - Add a delayed-source test proving stale overview loads cannot overwrite a newer period.
   - Assert a bounded query count for a large period; reporting must not issue per-day repository loads.

3. [x] Delete obsolete production files after `rg` finds no remaining callers:
   - `StorageService.kt`
   - `TimeService.kt`
   - `ProjectService.kt`
   - `TaskService.kt`
   - `EndOfDayService.kt`
   - `DeleteDayUseCase.kt`

4. [x] Rename `UpdateDayUseCase.kt` to `DayEditingUseCase.kt`, because the file now defines `DayEditingUseCase`.

5. Run verification after each migration cycle and once at the end:

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```

## Completion criteria

- No source or test code references the deleted services.
- View models take focused use cases, not repositories or the storage façade.
- Architecture tests reject UI dependencies on `data.repository`, `data.storage`, and generated database packages.
- Shared JVM tests, Android host tests, and the Android debug build pass.
