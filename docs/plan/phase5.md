### 📋 **Umsetzungsplan: Work Time to Track**

*Einfaches Zeit-Tracking-Tool für Android/Desktop (Kotlin Multiplatform)*

---
---

## **📌 Phase 5: Erweiterte Features**

**Ziel:** Optionalere Features umsetzen.

---

| **Aufgabe**                            | **Beschreibung**                                                                    | **Ergebnis**           | **Abhängigkeiten** | **Geschätzter Aufwand** |
|----------------------------------------|-------------------------------------------------------------------------------------|------------------------|--------------------|-------------------------|
| **5.1 PDF-Generierung implementieren** | Bibliothek für PDF-Erstellung integrieren (z. B. **iText** oder **Apache PDFBox**). | ✅ PDF-Generierung mit: 

- Titel "Zeitreport: [Datum von] – [Datum bis]".
- Tabelle mit Spalten: Datum, Projekt, Task, Start, Ende, Dauer, Notizen.
- Zusammenfassung: Gesamtzeit, Über-/Unterstunden.
- Styling: Lesbare Schriftgrößen, Ränder, Farben. | 4.10 | 6–8 Stunden |
  | **5.2 I18N (Internationaleisierung)** | Unterstützung für Deutsch und Englisch. | ✅ Alle UI-Texte in `strings.xml` (Android) und `strings.conf` (Desktop) externalisiert.
- Sprachauswahl im Menü (z. B. über Konfiguration).
- Dynamischer Wechsel ohne Neustart. | 1.2 | 4–6 Stunden |
  | **5.3 Dark Mode** | Theme-Wechsel zwischen Light und Dark Mode. | ✅ Compose-Themes für:
- Light Mode (Standard).
- Dark Mode (dunkle Hintergründe, helle Texte).
- Umschalter im Menü oder Systemeinstellungen folgen. | 1.4 | 4–6 Stunden |
  | **5.4 Cloud-Speicher (optional)** | Synchronisation mit Cloud (z. B. Firebase, Dropbox). | ✅ **Nicht priorisiert**, aber vorbereitet:
- Interface `CloudStorageService` mit Methoden für `sync()` und `backup()`.
- Lokale Daten werden mit Cloud synchronisiert. | 2.4 | 10–15 Stunden |

---
---