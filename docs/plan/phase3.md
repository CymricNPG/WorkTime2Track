### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

---

---

## **📌 Phase 3: Kernfunktionalität (Business-Logik)**

**Ziel:** Alle Zeit-Tracking- und Berechnungslogiken implementieren.

---

| **Aufgabe**                        | **Beschreibung**                                                        | **Ergebnis**                      | **Abhängigkeiten** | **Geschätzter Aufwand** |
|------------------------------------|-------------------------------------------------------------------------|-----------------------------------|--------------------|-------------------------|
| **3.1 TimeService implementieren** | Service für Zeitberechnungen, Buchungslogik und Überlappungsbehandlung. | ✅ `TimeService` mit Methoden für: 

- **Starten einer Buchung:** `startBooking(taskId: UUID, date: LocalDate, startTime: LocalTime)`
- **Beenden einer Buchung:** `endBooking(taskId: UUID, endTime: LocalTime)`
- **Suspendieren einer Buchung:** `suspendBooking(taskId: UUID, suspendTime: LocalTime)`
- **Fortsetzen einer Buchung:** `resumeBooking(taskId: UUID, resumeTime: LocalTime)`
- **Mergezeit-Logik:** Falls eine Buchung ≤5 Minuten vorher beendet wurde, starte neue Buchung mit dem Endzeitpunkt der vorherigen.
- **Überlappungsbehandlung:** Bei Überlappungen alte Buchung schließen und neue starten.
- **Berechnung gearbeiteter Zeit:** `getWorkedTime(date: LocalDate): Duration`
- **Berechnung Über-/Unterstunden:** `getOvertime(date: LocalDate): Duration` (gearbeitete Zeit – Sollzeit).
- **Freie Zeiten ignorieren:** Buchungen mit `Task.freeTime == true` werden nicht in die gearbeitete Zeit einbezogen. | 2.4 | 12–16 Stunden |
  | **3.2 Projekt- und Task-Verwaltung** | Logik für das Erstellen, Bearbeiten, Schließen und Löschen von Projekten/Tasks. | ✅ `ProjectService` und `TaskService` mit Methoden für:
- **Projekte:** `createProject`, `updateProject`, `closeProject`, `deleteProject` (nur wenn keine Buchungen existieren).
- **Tasks:** `createTask`, `updateTask`, `closeTask`, `deleteTask` (nur wenn keine Buchungen existieren).
- **Validierung:** Eindeutige Namen, keine leeren Namen, `closed`-Flag Handling.
- **Projektlose Tasks:** Spezielle Behandlung für Tasks ohne Projekt (`projectId = null`). | 2.4 | 8–10 Stunden |
  | **3.3 Feierabend-Logik implementieren** | Logik für den Feierabend-Screen: Sollzeit speichern, Tag abschließen. | ✅ `EndOfDayService` mit Methoden für:
- **Sollzeit setzen:** `setTargetTime(date: LocalDate, minutes: Int)`
- **Tag abschließen:** `endDay(date: LocalDate)` (aktive Buchung beenden, Sollzeit speichern).
- **Prüfung:** Feierabend nur möglich, wenn eine Buchung für den aktuellen Tag existiert. | 3.1 | 4–6 Stunden |
  | **3.4 Notizen-Verwaltung** | Logik für das Hinzufügen von Notizen zu Buchungen. | ✅ Erweiterung von `TimeService`:
- `addNoteToTime(timeId: UUID, note: String)`
- `removeNoteFromTime(timeId: UUID, note: String)`
- Notizen werden als Liste in `Time.description` gespeichert. | 3.1 | 2–4 Stunden |

---