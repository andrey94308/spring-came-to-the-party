# Live-Demo: Dokumentensuche in Postman

Dieses Experiment läuft **nur gegen das lokale, absichtlich verwundbare Demo-System**. Ziel ist ein kurzer Vorher/Nachher-Vergleich, keine angeblich zwingende Vier-Flag-Angriffskette.

## Vorbereitung

1. Im Projektordner `docker compose up -d --build --wait` ausführen.
2. `MATO-document-search.postman_collection.json` in Postman importieren.
3. Die Collection nutzt `http://localhost:8080`, Benutzer `bob` und das Demo-Passwort `sunshine`. Falls nötig, die Collection-Variablen anpassen.
4. Vor dem Vortrag die vier Requests einmal in Reihenfolge senden. Die Tests im Postman-Tab **Test Results** müssen grün sein.

## Ablauf (ca. 6–8 Minuten)

| Schritt | Frage ans Publikum | Erwartete Antwort |
| --- | --- | --- |
| 1. Normale Suche | „Welche Dokumente darf Bob sehen?“ | Nur `intel-analysis`. |
| 2. Fremde Abteilung | „Reicht es, den Namen einer geheimen Abteilung einzutragen?“ | Nein, `[]`. Das Recht kommt aus Bobs Benutzerkonto auf dem Server. |
| 3. SQL Injection | „Was ändert dieses zusätzliche `' OR '1'='1`?“ | Die verwundbare Suche liefert auch ein Dokument aus `mato-special-ops`. |
| 4. Sichere Suche | „Was passiert mit genau demselben Input, wenn Werte als SQL-Parameter übergeben werden?“ | `[]`. Der Input bleibt ein Wert und verändert die SQL-Logik nicht. |

Nach Schritt 3 kurz den Weg zeigen: `department` im HTTP-Body → `MariaDBFilterExpressionConverter` aus Spring AI 1.0.3 → zusammengesetztes SQL → zusätzliche Zeilen. Die Version ist von [CVE-2026-22730](https://spring.io/security/cve-2026-22730/) betroffen. Die Anwendung fügt den erzeugten Ausdruck absichtlich in ihre Abfrage ein; genau diese Einbindung ist eine Voraussetzung der Demo.

Wichtig bei Fragen: Der `query`-Wert im alten API-Vertrag wird in dieser Version des Projekts nicht verwendet. Die Postman-Demo zeigt einen Abteilungsfilter, keine Volltextsuche. Der „sichere“ Vergleichs-Endpoint verwendet Parameterbindung; er ist keine Aussage darüber, wie der Patch in Spring AI intern implementiert ist.

## Wenn die Live-Demo hakt

- Ist der Stack bereit? `docker compose ps` zeigt die Services und ihre Health-Checks.
- `401`: Collection-Authentifizierung und Variablen `demoUser`/`demoPassword` prüfen.
- `404`: Gateway unter Port 8080 und den Endpoint-Pfad prüfen.
- Unerwartete Daten: Der Seeder läuft nur bei leerer Datenbank. Für einen frischen Lauf `docker compose down -v` und anschließend `docker compose up -d --build --wait` ausführen. Dadurch werden die lokalen Demo-Daten gelöscht.

Für eine Präsentation einen Screenshot der vier erwarteten Antworten bereithalten. Die Screenshots sollten erst nach einem erfolgreichen lokalen Test aufgenommen werden.
