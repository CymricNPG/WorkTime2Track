# Integration test framework

`IntegrationTestEnvironment` provides a real SQLDelight database, repositories, and domain services
for tests in `shared/src/commonTest`.

## In-memory tests

```kotlin
@Test
fun example() = runTest {
    IntegrationTestEnvironment.create().use { environment ->
        val project = environment.createProject("Project")
        val task = environment.createTask("Task", project)

        environment.timeService.startBooking(
            taskId = task.id,
            date = LocalDate(2026, 6, 21),
            startTime = LocalTime(8, 0),
        )
    }
}
```

The environment exposes:

- `database`
- `storageService`
- `projectService`
- `taskService`
- `timeService`
- `endOfDayService`

Fixture helpers create projects, tasks, time entries, daily target times, and configuration values.

## Persistent databases

Use a persistent database when the generated data should be inspected in the desktop application:

```kotlin
val environment = IntegrationTestEnvironment.create(
    IntegrationTestDatabase.Persistent(
        path = "/absolute/path/integration.db",
        resetBeforeTest = true,
    ),
)
```

Close the environment before opening the same database in the desktop application.

Run the desktop application with that database:

```shell
WT2T_DATABASE_PATH=/absolute/path/integration.db ./gradlew :desktopApp:run
```

Alternatively, use the JVM system property:

```shell
./gradlew :desktopApp:run -Dwt2t.database.path=/absolute/path/integration.db
```

Both the test environment and desktop application use the same SQLDelight schema.
