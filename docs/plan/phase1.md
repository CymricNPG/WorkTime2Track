### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

---

---

---

## **📌 Phase 1: Projektgrundlage & Architektur**

**Ziel:** Technische Basis für die gesamte Anwendung schaffen.

---

| **Aufgabe**                          | **Beschreibung**                                                                                                                                                                                                  | **Ergebnis**                                                                                                                   | **Abhängigkeiten** | **Geschätzter Aufwand** |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|--------------------|-------------------------|
| **1.1 Projekt initialisieren**       | Kotlin Multiplatform Projekt (Android + Desktop) mit Gradle einrichten. Zielplattformen: Android und Desktop (JVM).                                                                                               | ✅ Funktionierendes KMP-Projekt mit `shared`, `androidApp` und `desktopApp` Modulen. Build-Konfiguration für beide Plattformen. | –                  | 4–8 Stunden             |
| **1.2 Abhängigkeiten konfigurieren** | Benötigte Bibliotheken einbinden: SQLite (z. B. **SQLDelight** oder **Room**), Kotlin Coroutines, Kotlin Serialization, Compose Multiplatform (für UI), PDF-Generierung (z. B. **iText** oder **Apache PDFBox**). | ✅ `build.gradle.kts` mit allen Abhängigkeiten für Shared-Code, Android und Desktop.                                            | 1.1                | 2–4 Stunden             |
| **1.3 MVVM-Architektur aufsetzen**   | Paketstruktur erstellen: `data` (Model, Repository), `domain` (Use Cases, Services), `ui` (ViewModel, Screens). Interfaces für `TimeService`, `StorageService` und `ProjectService` definieren.                   | ✅ Klare Projektstruktur mit:                                                                                                   

- `data/model/` (Datenklassen)
- `data/repository/` (Repository-Interfaces)
- `domain/service/` (Business-Logik-Interfaces)
- `ui/viewmodel/` (ViewModel-Basis)
- `ui/screen/` (Screen-Basis) | 1.1 | 4–6 Stunden |
  | **1.4 Basis-UI-Komponenten vorbereiten** | Shared UI-Komponenten für Compose Multiplatform erstellen (Buttons, Listen, Dialoge). Theme-System (Light/Dark Mode) grundlegend einrichten. | ✅ Wiederverwendbare Compose-Komponenten in `ui/components/`. Grundlegendes Theme in `ui/theme/`. | 1.1, 1.2 | 6–8 Stunden |