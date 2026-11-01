# Sprecherhinweise

1. Wir starten ohne Zugangsdaten. Das Ziel ist ein Dokument einer Abteilung ohne Benutzerkonto.
2. Nur der Gateway hat einen veröffentlichten Port. LDAP und MariaDB bleiben im Compose-Netz.
3. Die 401- und 403-Antworten sind die Ausgangslage. Eine normale Suche nach einer fremden Abteilung liefert keine Dokumente.
4. Hier wirken zwei Bedingungen zusammen: Spring LDAP 3.3.7 und die bewusst permissive OpenLDAP-Einstellung. Der leere Bind liefert keinen echten Identitätsnachweis. Der Datenbankeintrag verleiht danach die Admin-Rolle.
5. Die Hashes erscheinen durch eine absichtlich unsichere Admin-Antwort. Der Passwortvergleich findet lokal statt, nicht über einen HTTP-Endpunkt.
6. Bob und Maija teilen die Notizen der gleichen Abteilung. Die Abteilung kommt aus dem Serverdatensatz.
7. Vor der Injection zeigt die normale und die fremde Suche die Grenze.
8. Die Injection ändert den SQL-Ausdruck. Die sichere Vergleichsabfrage bindet beide Werte als Parameter. Sie ist kein Patch der Spring-AI-Bibliothek.
9. Die CVEs und die Entscheidungen der Anwendung getrennt benennen.
10. Nach Postman-Anfrage 3 kurz für den Offline-Vergleich pausieren, dann mit Bobs lokalem Passwort fortfahren.
