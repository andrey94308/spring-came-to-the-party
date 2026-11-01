# OrgArchive — Sprechernotizen

OrgArchive ist ein fiktiver Dienst für Notizen und Dokumente mehrerer Organisationen. Der öffentliche Gateway-Index zeigt die API. Ohne Anmeldung geben `/users` und `/notes` HTTP 401 zurück.

Ein Besucher registriert ein normales Konto in einer neuen Organisation. Der Zugriff auf Notizen einer anderen Organisation wird mit HTTP 403 verweigert. Danach erreicht das Konto durch CVE-2025-22223 eine Administratorfunktion: Spring Security 6.4.3 erkennt eine geerbte `@PreAuthorize`-Regel auf einem generischen Controller-Interface nicht. Das Konto bekommt keine Administratorrolle.

Der Debug-Modus der Benutzerliste enthält absichtlich BCrypt-Hashes. Bobs schwaches Laborpasswort wird offline gefunden. Als Bob zeigt die Demo geteilte Notizen und die normale Dokumentgrenze. Eine zweite Schwachstelle im Spring-AI-Metadatenkonverter erlaubt eine SQL-Injektion; die parametrisierte Vergleichsabfrage bleibt auf die Organisation beschränkt.
