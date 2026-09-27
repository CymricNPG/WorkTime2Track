# Aufgaben

- [ ] 
  Impressum https://chatgpt.com/g/g-p-6a2c7109e47c81919e327499f3ac67b6-worktime/c/6a67ba54-2124-83ed-af26-ce1fa05c46e4
- [ ] Datenschutzerklärung einfügen
- [ ] PDF Report : Zeitraum auswählen (Monate)
- [ ] PDF: Defaultnamen für PDF in Konfig festlegen
- [x] Bei einem aktiven Task neben der Startzeit ein kleinen +5/-5 Button
- [x] Neuen tag hinzufügen in Übersicht
- [x] Gesamten Tag löschen
- [ ] back sollte nicht discard machen -> Warnung anzeigen?
- [x] Wenn man einen Tag editiert hat und dann zurück zur Übrsicht geht, wird der Tag nicht aktualisiert
- [ ] Fehler Booking end 23:04 must be after start 23:04 on 2026-08-27
- [ ] Andere Icons für einen Task/Pause

## Code review findings — 2026-08-22

- [x] **Critical — Validate imported configuration and parse persisted settings safely.**
    - Current behavior: Database import validates entities and relationships but accepts arbitrary configuration keys
      and values. Application startup and the configuration screen parse the persisted theme with `ThemeMode.valueOf()`.
    - Impact: A corrupt or modified backup containing an unknown theme value can import successfully and then crash
      every subsequent application launch, leaving the user unable to repair the setting from inside the application.
    - Expected correction: Allowlist supported configuration keys, validate each value and numeric range during import,
      and use safe parsing with documented defaults when reading persisted settings.
    - Verification: Add tests for unknown keys, invalid themes, unsupported languages, malformed numeric values, and
      successful startup after importing a valid backup.

- [x] **High — Make legacy database migrations tolerant of previously valid data.**
    - Current behavior: Migration 3 copies legacy rows directly into replacement tables that introduce new constraints
      for identifiers, names, time ranges, and daily minute values. Older schemas allowed values that violate those
      constraints.
    - Impact: One legacy duplicate or invalid row can make migration fail on every startup, preventing an existing
      installation from opening after an upgrade.
    - Expected correction: Define deterministic normalization, deduplication, or quarantine rules before constrained
      rows are copied. Preserve the original database if migration cannot complete and report a recoverable error.
    - Verification: Exercise an actual on-disk upgrade for every class of data that was legal under the old schema but
      is rejected by the new schema, including duplicate names and invalid time ranges.

- [x] **High — Make booking transitions atomic and concurrency-safe.**
    - Current behavior: Starting a booking reads the current state, closes an active entry, loads configuration, and
      inserts the new entry through separate operations. Rapid taps can launch concurrent transitions, and the schema
      does not prevent multiple active entries.
    - Impact: Concurrent commands can create overlapping or multiple active bookings. A failure between writes can also
      leave the day partially updated with no active booking.
    - Expected correction: Move the complete switch operation into one transactional repository or use case, serialize
      booking commands, validate chronological boundaries, and add an appropriate database invariant for active entries.
    - Verification: Add concurrent-start, rapid-tap, rollback, clock-moving-backward, and invalid end-before-start tests
      using the production SQLDelight implementation.

- [x] **High — Bind view models and their collectors to navigation lifecycles.**
    - Current behavior: `AppContent` constructs AndroidX `ViewModel` instances with `remember`. Several of those view
      models start permanent `viewModelScope` collectors, but no `ViewModelStore` clears them when a navigation branch
      leaves composition.
    - Impact: Navigating away can leave database observers and coroutines alive. Reopening a screen creates another view
      model and another collector, causing memory leaks, duplicate work, and stale background updates.
    - Expected correction: Create view models through lifecycle-aware or Koin view-model APIs and scope them to stable
      navigation entries. Collect UI state with lifecycle awareness on supported targets.
    - Verification: Add navigation lifecycle tests proving that collectors are cancelled when their destination is
      removed and that revisiting a screen creates exactly one active observer.

- [x] **High — Move blocking database, PDF, and file operations off the UI thread.**
    - Current behavior: Most SQLDelight write methods are synchronous despite being declared `suspend` and do not switch
      to an I/O dispatcher. Desktop PDF generation and backup file reads and writes are also performed directly from
      UI-triggered call paths.
    - Impact: Database writes, report generation, or large backup transfers can block Compose rendering and cause
      visible freezes or Android ANRs.
    - Expected correction: Make repository and platform service APIs main-safe, inject a qualified I/O dispatcher, and
      perform all blocking driver, PDF, and filesystem work on it. Keep UI-state updates on the main dispatcher.
    - Verification: Add dispatcher tests or strict-mode checks showing that blocking operations do not execute on the UI
      thread, plus a large-file/report smoke test.

- [x] **High — Correct the end-of-day workflow and transaction boundary.**
    - Current behavior: The end-of-day action is always enabled, although the service rejects a new end-of-day without
      bookings. The resulting exception is not represented in UI state. The configured default target duration is
      ignored in favor of a hardcoded 480 minutes, and closing the booking and saving the target are separate
      operations.
    - Impact: Users can enter a workflow that fails without useful feedback, configured defaults do not work, and
      partial failures can close a booking without saving the intended daily target.
    - Expected correction: Derive action eligibility from current-day state, load the configured default when no daily
      value exists, expose progress and error state, prevent duplicate submissions, and persist booking closure plus
      daily values atomically.
    - Verification: Cover no-booking eligibility, configured defaults, existing daily values, save failure rollback,
      duplicate taps, and successful completion in service and view-model tests.

- [ ] **Medium — Replace daily-overview N+1 queries and cancel stale loads.**
    - Current behavior: The overview first loads complete time and target lists, then performs roughly six additional
      repository queries per displayed day through repeated `getTimesForDate()`, `getWorkedTime()`, and `getOvertime()`
      calls. Each period update starts a new uncancelled load job.
    - Impact: A one-year history can cause more than 2,000 database queries. A slower earlier request can finish after a
      newer period request and overwrite the displayed entries with stale results.
    - Expected correction: Build the overview from one consistent snapshot or a dedicated aggregate query/use case,
      derive totals once, and use cancellable latest-request semantics such as `flatMapLatest` or an explicitly managed
      job.
    - Verification: Assert bounded query counts for a large history and test rapid period changes with delayed
      repositories to prove that only the newest result reaches UI state.

- [x] **Medium — Connect Android system Back to the shared navigation stack.**
    - Current behavior: Only toolbar callbacks invoke `NavigationStack.pop()`; Android system Back is not handled by the
      shared application shell.
    - Impact: Pressing system Back on a nested destination exits or backgrounds the activity instead of returning to the
      previous screen, contrary to the documented navigation workflow.
    - Expected correction: Register lifecycle-aware Android Back handling that pops the shared stack when history exists
      and delegates to the activity only at the root destination. Define the same discard or confirmation behavior as
      the visible Back action.
    - Verification: Add Android navigation tests for nested destinations, the root destination, and screens with unsaved
      drafts.

- [x] **Medium — Prevent unsaved configuration drafts from changing application state.**
    - Current behavior: Selecting a theme immediately updates the application-level theme state before the user presses
      Save. Navigating Back discards persistence but does not restore the previous applied theme.
    - Impact: The visible application state no longer matches persisted configuration, violating the documented
      save/discard contract and creating surprising behavior after navigation or restart.
    - Expected correction: Apply theme and language only after successful persistence, or explicitly restore the
      original values when the draft is discarded.
    - Verification: Test saving, toolbar Back, Android system Back, and persistence failures for both theme and language
      changes.

- [x] **Architecture — Replace the pass-through `StorageService` with feature-oriented boundaries.**
    - Current behavior: `StorageService` aggregates six repositories and primarily forwards their operations. Some view
      models depend on this façade and several domain services simultaneously, allowing presentation code to choose
      between multiple persistence paths.
    - Impact: Layer ownership is unclear, transaction boundaries are scattered, dependencies grow with unrelated
      features, and duplicated calculations such as the overview query pattern become easy to introduce.
    - Expected correction: Expose focused commands and query use cases for booking, day editing, configuration, project
      management, reporting, and backup. Let each use case depend directly on the minimum repository contracts it needs.
    - Verification: Document the dependency direction, reduce view-model constructor dependencies, and add architecture
      tests preventing UI packages from accessing low-level storage abstractions.

- [x] **Architecture — Remove or platform-isolate the filesystem export overload from `commonMain`.**
    - Current behavior: `DatabaseExportRepository` imports `java.io.File` and exposes an unused path-based export
      overload even though launchers already own native file selection and writing.
    - Impact: Shared persistence code is coupled to JVM filesystem semantics, weakens the platform boundary, and makes
      future non-JVM targets unnecessarily difficult.
    - Expected correction: Keep shared export byte- or content-oriented and perform destination selection and writing in
      platform launchers or explicit platform adapters.
    - Verification: Confirm there are no Java or Android filesystem imports in `commonMain` and test the shared
      serializer independently from platform file transfer.

- [x] **Build architecture — Generate the SBOM outside tracked source resources.**
    - Current behavior: Ordinary resource and test tasks run CycloneDX generation directly into a tracked
      `composeResources` file. The generated SBOM contains a random serial number, timestamp, and host-specific desktop
      dependencies.
    - Impact: Normal builds dirty the working tree, developers on different operating systems overwrite each other's
      output, and committed artifacts are not reproducible.
    - Expected correction: Generate the SBOM under the Gradle build directory, package that generated output into
      application resources, and define whether release artifacts need normalized or target-specific SBOMs.
    - Verification: Run the build twice and on supported host platforms, confirm the source tree remains clean, and
      verify that each packaged application can still load its bundled SBOM.

- [ ] **Testing — Add platform and integration coverage for the reviewed risks.**
    - Current behavior: Shared JVM and Android host tests pass, but the Android-specific test is only a placeholder.
      There is no meaningful coverage for platform file/PDF behavior, system Back, navigation lifecycle cleanup,
      concurrent booking commands, corrupt settings, or invalid legacy migrations.
    - Impact: The existing suite can remain green while production-only lifecycle, threading, migration, and platform
      failures regress.
    - Expected correction: Add focused Android host/device tests, desktop integration tests, real SQLDelight concurrency
      and migration tests, and Compose navigation/lifecycle tests. Replace placeholder arithmetic tests with
      behavior-focused coverage.
    - Verification: Ensure each risk above has a regression test on the lowest appropriate layer and run both
      `:shared:jvmTest` and `:shared:testAndroidHostTest` in CI.