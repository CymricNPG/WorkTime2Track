### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

---

---

---

## **📌 Phase 2: Datenmodell & Persistenz**

**Ziel:** Datenmodell implementieren und Persistenzschicht aufbauen.

---

| **Aufgabe**                         | **Beschreibung**                                                                                                                                                                    | **Ergebnis**        | **Abhängigkeiten** | **Geschätzter Aufwand** |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|--------------------|-------------------------|
| **2.1 Datenklassen implementieren** | Kotlin-Datenklassen für `Time`, `Task`, `Project` und `DailyWorkTime` erstellen. UUID-Generierung, Validierung (z. B. leere Namen verbieten), und `closed`-Flag für Projekte/Tasks. | ✅ Datenklassen mit: 

- `Time(id: UUID, date: LocalDate, start: LocalTime, end: LocalTime?, description: List<String>)`
- `Task(id: UUID, name: String, freeTime: Boolean, closed: Boolean, projectId: UUID? = null)`
- `Project(id: UUID, name: String, closed: Boolean)`
- `DailyWorkTime(id: UUID, date: LocalDate, minutes: Int)`
- Hilfsfunktionen für Serialisierung/Deserialisierung. | 1.1 | 4–6 Stunden |
  | **2.2 Datenbank-Schema erstellen** | SQLite-Tabellen für alle Entitäten definieren (z. B. mit SQLDelight oder Room). Indizes für Performance (z. B. `date` in `Time` und `DailyWorkTime`). | ✅ SQL-Schema mit:
- Tabelle `times` (id, date, start, end, task_id, description)
- Tabelle `tasks` (id, name, free_time, closed, project_id)
- Tabelle `projects` (id, name, closed)
- Tabelle `daily_work_times` (id, date, minutes)
- Migrationen für zukünftige Änderungen vorbereiten. | 1.2, 2.1 | 6–8 Stunden |
  | **2.3 Repository-Schicht implementieren** | `TimeRepository`, `TaskRepository`, `ProjectRepository` und `DailyWorkTimeRepository` erstellen. CRUD-Operationen für alle Entitäten. | ✅ Repository-Klassen mit Methoden für:
- Erstellen, Lesen, Aktualisieren, Löschen von Entitäten.
- Abfragen nach Datum, Projekt, Task, etc.
- Transaktionsunterstützung für komplexe Operationen. | 2.2 | 8–12 Stunden |
  | **2.4 Storage-Service implementieren** | `StorageService` als Wrapper für die Repositorys. Verantwortlich für das Speichern/Laden des gesamten Datenmodells. | ✅ `StorageService` mit Methoden wie:
- `saveTime(time: Time)`
- `getTimesForDate(date: LocalDate): List<Time>`
- `getOpenProjects(): List<Project>`
- `getTasksForProject(projectId: UUID): List<Task>`
- `getDailyWorkTime(date: LocalDate): DailyWorkTime?` | 2.3 | 4–6 Stunden |

---