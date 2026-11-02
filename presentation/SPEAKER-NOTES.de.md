# OrgArchive — Sprechernotizen (DE)

Kurze Punkte zum freien Sprechen. Kein Text zum Vorlesen.

## 1. Einstieg: Warum dieses Demo?

- Ihr habt sicher von den zwei neuen Sicherheitslücken in PHP gehört. Unter bestimmten Bedingungen konnte ein Angreifer damit ohne Login Code auf dem Server ausführen. Diesen Fall bespreche ich heute nicht.
- Heute zeige ich ein anderes Beispiel: mein kleines Demo-Projekt OrgArchive mit zwei schon bekannten CVEs in seinen Libraries.
- Ich starte ohne Account und ohne Login. Dann registriere ich einen normalen User. Schritt für Schritt komme ich zur User-Liste und zu allen Documents in der Datenbank.
- Ich zeige, wie CVEs und Entscheidungen im Code zusammen den Zugriff auf diese geschützten Daten ermöglichen.
- Zuerst erkläre ich das Projekt und seine Security. Danach zeige ich wichtige Stellen im Code und die Requests in Postman.

## 2. OrgArchive: Daten und Suche

- In OrgArchive speichern verschiedene Organizations ihre Documents.
- Ein User soll nur Documents seiner eigenen Organization sehen. Ein Admin kann Users und Organizations ansehen.
- User und Document haben beide eine eigene Spalte `organizationUuid`. Sie verbindet sie mit der Organization. In den JSON-Metadaten des Documents stehen `category` und `revision`.
- Ich habe MariaDB 11.8 gewählt, weil sie Vektoren unterstützt. So könnte ich später ähnliche Documents anhand ihres Textes finden.
- Aus demselben Grund nutze ich Spring AI. Es kann ein Modell für Textvektoren mit MariaDB und der Suche verbinden.
- Im aktuellen Projekt nutze ich nur einen kleinen Teil von Spring AI: den Filter für Metadaten. Eine Vektorsuche gibt es noch nicht.

## 3. API: Welche Endpoints gibt es?

- Der Aufbau ist einfach: Postman → Archive Service auf `localhost:8080` → MariaDB in Docker.
- `GET /v3/api-docs` liefert die OpenAPI-Spezifikation. Dort sehen wir Pfade und HTTP-Methoden. Die Liste gibt keinen Zugriff auf geschützte Daten.
- `POST /signup` ist öffentlich. Der Server erstellt einen User und eine neue interne `organizationUuid`. Die Antwort enthält nur den Username.
- `GET /users` und `GET /users/{id}` sind für Admins gedacht. `GET /organizations` und `GET /organizations/{id}` nutzen dieselbe Regel.
- `POST /documents/search` ist für Users. Der Server liest die Organization-UUID aus dem Account, nicht aus dem Request.
- Mit `search-safe` zeige ich später den sicheren Vergleich. `search-debug` gibt es für das Demo, aber wir brauchen es nicht in der Hauptkette.

## 4. Code: Wo wird der Zugriff geprüft?

- Ich öffne `SecurityConfig`. HTTP Basic prüft Username und Passwort. Ohne Login sind nur `/signup` und OpenAPI erreichbar.
- Für `/users`, `/organizations` und `/documents` verlangt der HTTP-Filter einen Login. Danach prüft die Methode die Rolle.
- Ich öffne `AdminRoleSecurityContract`. Das Interface definiert `list` und `get`. Beide Methoden haben `@PreAuthorize("hasRole('ADMIN')")`.
- `UserController` und `OrganizationController` implementieren dieses Interface. So sollen beide dieselbe Admin-Regel nutzen.
- Ich öffne `DocumentController`: Für die Suche braucht man die Rolle `USER`. In `DocumentSearchService` kommt die Organization-UUID aus dem Account des Users.
- Passwörter liegen als BCrypt-Hashes in der Datenbank. Spring Security prüft die Rollen `USER` und `ADMIN`.

## 5. Code: Der Debug-Fehler

- In `UserController` gibt `GET /users` keine Passwörter zurück. Mit `GET /users?debug=true` enthält die Antwort aber `passwordHash` für jeden User.
- Diesen Fehler habe ich für das Demo eingebaut. Älteren Debug-Code dieser Art kann man auch in echten Projekten finden.
- Zuerst wirkt das Risiko klein: Der Endpoint ist für Admins gedacht. Außerdem sind es Hashes und keine Passwörter im Klartext.

## 6. Zwei CVEs

- CVE-2025-22223: Unsere Spring-Security-Version findet `@PreAuthorize` auf dem generischen Interface unter diesen Bedingungen nicht.
- Der HTTP-Filter verlangt weiter einen Login. Aber ein normaler `USER` kann `/users` ohne `ADMIN`-Rolle lesen. Das ist der erste Zugriff ohne die nötige Berechtigung.
- CVE-2026-22730: Der Spring-AI-Converter setzt den Wert von `category` ohne korrektes Escaping in SQL ein. Eine zusätzliche `OR`-Bedingung umgeht den Filter für die Organization.
- Nach der Injection sehe ich Documents anderer Organizations.

## 7. Fazit

- Ich prüfe regelmäßig meine Dependencies und neue CVEs.
- Ich schaue auf den konkreten Code im Projekt. So kann ich das Risiko bewerten und wichtige Updates zuerst machen.
- Alte Legacy-Entscheidungen können zusammen mit einer neuen CVE zu einem ernsten Problem werden.

Quelle für den Einstieg: [PHP Security Advisory vom 21. Oktober 2026](https://www.php.net/archive/2026.php).
