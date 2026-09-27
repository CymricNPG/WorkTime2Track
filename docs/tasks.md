# Aufgaben

- [ ] Impressum
- [ ] Datenschutzerklärung
- [ ] PDF Report: Zeitraum auswählen (Monate)
- [ ] PDF: Defaultnamen für PDF in Konfig festlegen
- [ ] back sollte nicht discard machen -> Warnung anzeigen?
- [ ] Fehler Booking end 23:04 must be after start 23:04 on 2026-08-27
- [ ] Andere Icons für einen Task/Pause

## Code review findings — 2026-08-22

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