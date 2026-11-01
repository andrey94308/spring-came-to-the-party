# OrgArchive — Sprechernotizen

1. **Einführung.** OrgArchive ist ein fiktiver Dienst für Notizen und Dokumente mehrerer Organisationen. Zuerst erkläre ich Architektur und zwei konkrete CVEs; danach führe ich die 16 Anfragen in Postman aus.

2. **Architektur.** Von außen ist nur das Gateway auf Port 8080 erreichbar. Es leitet API-Anfragen an den internen Archive Service weiter. Dieser authentifiziert mit HTTP Basic gegen BCrypt-Hashes in MariaDB. Dort liegen auch Notizen und Dokumente.

3. **Endpunkte.** `GET /` zeigt für die Vorführung eine öffentliche API-Übersicht; `POST /signup` ist ebenfalls öffentlich. Die übrigen Endpunkte verlangen eine Anmeldung. Für `/users` und `/organizations` ist zusätzlich eine Admin-Rolle vorgesehen. Die Angaben `access` im Gateway sind nur Text, keine Sicherheitsregeln. Ohne diesen Index könnte man Routen etwa in API-Dokumentation, Client-Code, beobachtetem Netzwerkverkehr oder durch gezielte Anfragen finden. Eine vollständige Liste folgt daraus nicht automatisch. Das Wissen um einen Pfad darf keinen Zugriff gewähren.

4. **CVE-2025-22223.** `@EnableMethodSecurity` ist aktiv. `AdminQuery<F,R>.list` trägt `@PreAuthorize("hasRole('ADMIN')")`; die konkreten Controller überschreiben die Methode ohne eigene Annotation. Spring Security 6.4.3 erkennt diese geerbte Regel auf dem generischen Interface nicht zuverlässig. Der HTTP-Filter verlangt weiterhin Authentifizierung. Ein frisch registrierter `ROLE_USER` kann so die Benutzerliste lesen, erhält aber keine Admin-Rolle. Spring Security 6.4.4 behebt den Fehler. Quelle: https://spring.io/security/cve-2025-22223/

5. **Zusätzliche Fehler.** Die normale Benutzerliste enthält keine Passwort-Hashes. Mit `debug=true` liefert sie BCrypt-Hashes, was ein Fehler der Anwendung ist. Bobs schwaches Laborpasswort wird anhand seines Hashes offline gefunden; das ist kein Bruch von BCrypt. Die direkte Route `/users/bob` besitzt ihre eigene Admin-Annotation und antwortet für den Besucher mit 403.

6. **Organisationsgrenze.** Die Registrierung erzeugt ein normales Konto in einer neuen Organisation. Eigene Notizen sind abrufbar, fremde Notizen liefern 403. Bob und Maija gehören zu `atlas-consulting` und teilen Notizen; Renaud gehört zu `harbor-delivery`. Eine gewöhnliche Dokumentsuche nach einer fremden Organisation liefert eine leere Liste.

7. **SQL-Aufbau.** Der Archive Service liest Bobs Organisation aus dem gespeicherten Benutzerkonto und bindet sie als JDBC-Parameter. Daneben erzeugt Spring AIs `MariaDBFilterExpressionConverter` einen SQL-Ausdruck aus dem vom Client gelieferten Metadatenfilter. Die Anwendung hängt diesen Ausdruck als SQL-Text hinter `AND` an. Der Request-Parameter `query` wird in dieser Demo nicht benutzt; relevant ist `organization`.

8. **CVE-2026-22730.** In Spring AI MariaDB Store 1.0.3 wird der Filterwert ohne korrektes Escaping in den SQL-Ausdruck eingesetzt. Der Wert `atlas-consulting' OR '1'='1` verändert dadurch die Bedingung. Da SQL `AND` vor `OR` auswertet, kann der neue `OR`-Teil die Organisationsgrenze umgehen. Die Antwort enthält Dokumente anderer Organisationen. Version 1.0.4 behebt den Fehler. Quelle: https://spring.io/security/cve-2026-22730/

9. **Vergleich und Abhilfe.** `/documents/search-safe` bindet beide Werte als JDBC-Parameter und liefert für denselben manipulierten Wert eine leere Liste. Das ist ein eigener Vergleichsendpunkt, kein gepatchter Spring-AI-Code. Zusätzlich gehören die Bibliotheken aktualisiert, die Rollenprüfung direkt an die konkrete Methode und Passwort-Hashes nicht in API-Antworten.

10. **Postman.** Anfragen 1–6: Index, anonyme 401-Antworten, Registrierung und Notizengrenze. 7–12: Rollen-Bypass, Debug-Hash, lokale Passwortsuche und geteilte Notizen. 13–16: normale Suche, fremde Organisation, SQL-Injektion und sichere Vergleichsabfrage. Nach diesem Slide direkt in Postman wechseln.
