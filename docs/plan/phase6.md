### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

---

---

## **📌 Phase 6: Testing & Qualitätssicherung**

**Ziel:** Stabilität und Korrektheit der Anwendung sicherstellen.

---

| **Aufgabe**                           | **Beschreibung**                                                                | **Ergebnis** | **Abhängigkeiten** | **Geschätzter Aufwand** |
|---------------------------------------|---------------------------------------------------------------------------------|--------------|--------------------|-------------------------|
| **6.1 Unit Tests für Business-Logik** | Tests für `TimeService`, `ProjectService`, `TaskService` und `EndOfDayService`. | ✅ Tests für: 

- Zeitberechnungen (Überlappungen, Mergezeit, Über-/Unterstunden).
- Validierung (leere Namen, eindeutige Namen).
- Buchungslogik (Start/Stop/Suspend/Resume).
- Sollzeit-Berechnung.
- **Abdeckung:** ≥80% der Business-Logik. | 3.1–3.4 | 12–16 Stunden |
  | **6.2 UI Tests** | Tests für kritische UI-Workflows (z. B. Buchung starten/beenden). | ✅ Tests für:
- Buchungs-Screen: Task auswählen → Buchung starten → Task erneut auswählen → Buchung beenden.
- Feierabend-Screen: Sollzeit setzen → Tag abschließen.
- Projektverwaltung: Projekt erstellen → Task hinzufügen → Task löschen.
- **Tools:** Compose Testing (z. B. `ComposeContentTestRule`). | 4.1–4.10 | 8–10 Stunden |
  | **6.3 Integrationstests** | End-to-End-Tests für gesamte Workflows. | ✅ Tests für:
- Kompletter Tag: Buchung starten → Pause → Fortsetzen → Feierabend.
- Projekt/Task-Verwaltung: Erstellen → Bearbeiten → Löschen.
- Report-Generierung: PDF für Zeitraum erstellen und prüfen. | 6.1, 6.2 | 6–8 Stunden |
  | **6.4 Manuelles Testing** | Manuelle Tests auf Android und Desktop. | ✅ Getestete Szenarien:
- Alle UI-Screens und Navigation.
- Randfälle (z. B. Buchung über Mitternacht → Fehlermeldung).
- Performance (z. B. große Datenmengen).
- **Dokumentation:** Liste der getesteten Fälle und gefundenen Bugs. | Alle vorherigen | 8–10 Stunden |
  | **6.5 Bugfixing** | Gefundene Bugs aus Tests beheben. | ✅ Alle kritischen Bugs behoben.
- Priorisierung nach Schweregrad. | 6.1–6.4 | 4–8 Stunden |

---
---