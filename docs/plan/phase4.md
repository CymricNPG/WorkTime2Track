### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

## **📌 Phase 4: UI-Implementierung (Compose Multiplatform)**

**Ziel:** Alle Screens und UI-Workflows umsetzen.

---

| **Aufgabe**                 | **Beschreibung**                                                                   | **Ergebnis**      | **Abhängigkeiten** | **Geschätzter Aufwand** |
|-----------------------------|------------------------------------------------------------------------------------|-------------------|--------------------|-------------------------|
| **4.1 UI-State-Management** | ViewModel-Klassen für alle Screens erstellen. State-Holding mit Kotlin Flow/State. | ✅ ViewModels für: 

- `BookingViewModel` (aktuelle Buchung, Task-Liste, Notiz-Eingabe)
- `EndOfDayViewModel` (Sollzeit, gearbeitete Zeit, Speichern)
- `ProjectListViewModel` (Projektliste, Filter für geschlossene Projekte)
- `TaskListViewModel` (Task-Liste pro Projekt)
- `ProjectViewModel` (Projekt-Details, Bearbeiten)
- `TaskViewModel` (Task-Details, Bearbeiten)
- `ConfigViewModel` (Default-Sollzeit, Mergezeit)
- Jedes ViewModel nutzt `TimeService`, `ProjectService` und `TaskService`. | 3.1–3.4 | 10–12 Stunden |
  | **4.2 Buchungs-Screen** | Hauptscreen mit Task-Liste, Start/Stop/Suspend-Logik, Notiz-Eingabe. | ✅ Screen mit:
- **Task-Liste:** Sortiert nach zuletzt genutzt, Anzeige von Name + Projekt.
- **Aktiver Task:** Hervorgehoben, mit Startzeit.
- **Buttons:**
    - **Pause:** Suspendiert aktuelle Buchung, deaktiviert Task-Auswahl.
    - **Feierabend:** Öffnet Feierabend-Screen.
- **Notiz-Eingabe:** Textfeld + Button (nur aktiv, wenn Task aktiv).
- **Freie Tasks:** Mit Sonnenschirm-Icon gekennzeichnet.
- **Automatisches Speichern:** Änderungen werden sofort übernommen (kein Speicher-Button). | 4.1 | 8–10 Stunden |
  | **4.3 Feierabend-Screen** | Screen für Sollzeit-Eingabe und Tagesabschluss. | ✅ Screen mit:
- Titel **"Arbeitszeit"**.
- Anzeige von:
    - Aktuelles Datum.
    - Erste Buchungszeit des Tages.
    - Letzte Buchungszeit des Tages.
    - Gearbeitete Stunden:Minuten.
- Eingabefeld für Sollzeit (Default-Wert vorausgewählt).
- **Speichern-Button:** Speichert Sollzeit und schließt den Tag ab.
- **Zurück-Button:** Verwirft Änderungen. | 4.1 | 4–6 Stunden |
  | **4.4 Projektverwaltung-Screen** | Liste aller Projekte mit Bearbeiten/Löschen-Optionen. | ✅ Screen mit:
- **Projektliste:** Alle Projekte + Eintrag für projektlose Tasks (nicht löschbar).
- **Icons pro Projekt:**
    - **Bearbeiten:** Öffnet Projekt-Screen.
    - **Löschen:** Nur aktiv, wenn keine Buchungen für Tasks des Projekts existieren.
- **+-Button:** Öffnet Projekt-Screen zum Erstellen.
- **Sortierung:** Alphabetisch oder nach zuletzt genutzt. | 4.1 | 6–8 Stunden |
  | **4.5 Projekt-Screen** | Screen zum Bearbeiten/Erstellen eines Projekts. | ✅ Screen mit:
- Eingabefeld für Projektname (Validierung: nicht leer, eindeutig).
- Checkbox **"Projekt geschlossen"** (deaktiviert Anzeige im Buchungs-Screen).
- **Speichern-Button:** Speichert Änderungen.
- **Zurück-Button:** Verwirft Änderungen. | 4.1 | 4–6 Stunden |
  | **4.6 Taskverwaltung-Screen** | Liste aller Tasks eines Projekts. | ✅ Screen mit:
- Titel: Name des Projekts.
- **Task-Liste:** Alle Tasks des Projekts.
- **+-Button:** Öffnet Task-Screen zum Erstellen.
- **Icons pro Task:**
    - **Bearbeiten:** Öffnet Task-Screen.
    - **Löschen:** Nur aktiv, wenn keine Buchungen für den Task existieren. | 4.1 | 6–8 Stunden |
      | **4.7 Task-Screen** | Screen zum Bearbeiten/Erstellen eines Tasks. | ✅ Screen mit:
- Titel: Name des Projekts.
- Eingabefeld für Taskname (Validierung: nicht leer, eindeutig).
- Checkbox **"Freie Zeit"** (markiert Task als `freeTime`).
- Checkbox **"Task geschlossen"** (deaktiviert Anzeige im Buchungs-Screen).
- **Speichern-Button:** Speichert Änderungen.
- **Zurück-Button:** Verwirft Änderungen. | 4.1 | 4–6 Stunden |
  | **4.8 Menü- und Hilfescreens** | Konfiguration, Impressum, Hilfe. | ✅ Screens für:
- **Konfiguration:**
    - Default-Sollzeit (Stunden:Minuten).
    - Mergezeit (Standard: 5 Minuten).
    - **Speichern-Button.**
- **Impressum:** Statischer Text (z. B. aus `strings.xml`).
- **Hilfe:** Statischer Hilfetext.
- **Navigation:** Von jedem Screen mit Zurück-Button zum vorherigen Screen. | 4.1 | 4–6 Stunden |
  | **4.9 Arbeitstagübersicht-Screen** | Übersicht über gebuchte Zeiten pro Tag/Projekt/Task. | ✅ Screen mit:
- **Zeitraumauswahl:** Start- und Enddatum.
- **Anzeige:**
    - Gesamtzeit pro Tag.
    - Über-/Unterstunden pro Tag.
    - Zeit pro Projekt/Task (filterbar).
- **Export-Button:** Öffnet Report-Screen. | 4.1 | 6–8 Stunden |
  | **4.10 Report-Screen** | PDF-Report-Generierung für ausgewählten Zeitraum. | ✅ Screen mit:
- **Zeitraumauswahl:** Start- und Enddatum.
- **Vorschau:** Tabellarische Übersicht (wie Arbeitstagübersicht).
- **PDF generieren-Button:** Erstellt PDF mit:
    - Übersicht pro Tag.
    - Aufschlüsselung pro Projekt/Task.
    - Notizen zu Buchungen (falls vorhanden).
- **Speicherort:** PDF wird im Download-Ordner (Desktop) oder im App-Ordner (Android) gespeichert. | 4.1 | 8–10 Stunden |

---