# Sprechertext und Demo-Hinweise

## Slide 1: Wenn Schwachstellen

Guten Tag zusammen. Heute möchte ich euch ein kleines Security-Experiment zeigen. Ich habe ein Spring Demo-System untersucht, in dem mehrere Schwächen zusammenkommen. Mich hat interessiert, was ein Angreifer Schritt für Schritt beobachten und erreichen kann. Ich zeige die Architektur, vier Experimente und die Ergebnisse direkt im Terminal. Danach möchte ich mit euch darüber sprechen, welche Prüfungen für unsere Entwicklung hilfreich sind.

## Slide 2: Warum Updates wichtig sind

Zuerst die Frage: Warum sind Dependency Updates überhaupt wichtig? Der Anwendungscode kann gleich bleiben, und trotzdem kann sich das Risiko ändern. Zum Beispiel wird eine Schwachstelle in einer Library, einem Framework oder einer Runtime veröffentlicht. Dann reicht die Information „wir nutzen diese Technologie“ noch nicht. Wir müssen wissen, welche Version tatsächlich läuft und ob die betroffene Funktion von außen erreichbar ist. Erst dann können wir die Priorität und den nächsten Schritt sinnvoll beurteilen.

## Slide 3: Ein aktuelles Beispiel: PHP

Ein aktuelles Beispiel ist PHP. Am 21. Oktober 2026 hat das PHP-Projekt Updates für zwei kritische Schwachstellen veröffentlicht. Je nach Einsatz konnten sie Remote Code Execution ermöglichen. Ich möchte die PHP-Technik heute nicht im Detail erklären. Für diese Präsentation ist der praktische Prozess wichtig: Welche Versionen laufen? Wo ist PHP eingesetzt? Welche Systeme sind über das Web erreichbar? Und wie schnell können wir aktualisieren? Diese Fragen gelten auch für andere Plattformen.

Quelle: https://www.php.net/archive/2026.php#2026-10-21-5

## Slide 4: Warum heute Spring?

Bei uns basieren viele Projekte auf Java und Spring. Deshalb habe ich für den praktischen Teil ein Spring Demo-System gewählt. Es ist eine kontrollierte Umgebung und keine Aussage über unsere produktiven Systeme. Ich möchte zeigen, welche Fragen wir bei einer Schwachstelle stellen können: Wo steht die betroffene Komponente? Ist der Weg von außen erreichbar? Und was kann nach dem ersten erfolgreichen Schritt passieren?

## Slide 5: Agenda

Kurz zum Ablauf: Ich beginne mit der Geschichte und der Idee des Demo-Systems. Dann sehen wir uns die drei Services und den normalen Datenfluss an. Danach kommen vier Experimente. Bei jedem Experiment erkläre ich zuerst den Mechanismus und wechsle dann kurz in das Terminal. Am Ende fasse ich die Beobachtungen zusammen und nenne die Punkte, die ich für die Entwicklung wichtig finde.

## Slide 6: Die Geschichte in einer Minute

Das Demo-System hat eine kleine Spielgeschichte. In der fiktiven Organisation MATO gibt es ein Archiv. Ein AI Assistant hat versehentlich eine Konfigurationsdatei in ein öffentliches Repository gestellt. Darin findet man die Adresse des Gateway. Von dort beginnt die Untersuchung. Im Projekt gibt es vier Flags. Das sind nur Marker: Sie zeigen, dass eine Stufe erreicht wurde. Für uns ist nicht das Spiel selbst wichtig, sondern der Weg durch die Services und die Frage, welche Schwäche welchen Zugang ermöglicht.

## Slide 7: Die Idee des Demo-Systems

Die Idee war, eine kleine, kontrollierte Umgebung zu bauen. Ein Service verwaltet Benutzer, persönliche Notizen und Dokumente. Ein Gateway leitet die Archiv-Anfragen weiter. Ein weiterer Service hat einen eigenen Login- und OTP-Bereich. So kann ich nicht nur einzelne Fehler isoliert zeigen, sondern auch beobachten, was nach einem Datenabfluss möglich wird. Ich nutze absichtlich einfache Testdaten. Die Ergebnisse sind reproduzierbar und betreffen nur dieses Demo-System.

## Slide 8: Drei Services und eine Datenbank

Hier sehen wir die Architektur. Links steht die externe Anfrage. Der edge-gateway auf Port 8080 ist der öffentliche Zugang zum Archiv. Er leitet /users, /notes und /documents/search an den internen archive-service weiter. Dieser Service speichert seine Daten in MariaDB. Unten ist command-control. Dieser Service ist im Demo ebenfalls direkt erreichbar, aber er läuft unabhängig vom Gateway. Er enthält Login, OTP und einen mit JWT geschützten Endpunkt. Für die Experimente ist wichtig, an welchen Stellen externe Eingaben eine interne Entscheidung beeinflussen.

## Slide 9: Der normale Ablauf

Bevor ich die Schwächen zeige, schauen wir auf den normalen Ablauf. Die öffentliche /users-Antwort enthält nur allgemeine Informationen. Für /notes muss man sich mit Basic Auth anmelden; dann sieht man die eigenen Notizen. Bei /documents/search wird ein department-Wert als Filter verwendet. Im separaten command-control braucht man für den letzten Endpunkt einen gültigen JWT. Diese vier Antworten dienen als Vergleich. Nach jedem Experiment prüfen wir, was sich gegenüber diesem Ausgangszustand geändert hat. Wichtig: Der department-Filter ist im Demo keine vollständig durchgesetzte Benutzerberechtigung.

## Slide 10: Meine Forschungsfrage

Meine Forschungsfrage war: Wie weit kommt man, wenn in einer Anwendung mehrere Schwächen zusammenkommen? Ich habe für jeden Schritt denselben Ablauf benutzt. Zuerst halte ich den Startzugang fest. Dann ändere ich genau einen Input. Danach beobachte ich die Antwort und frage, ob daraus ein weiterer Schritt möglich wird. So lässt sich die Kette nachvollziehen. Gleichzeitig kann man sauber trennen, welche Schwäche aus einer Dependency kommt und welche durch den Anwendungscode oder die Konfiguration entsteht.

## Slide 11: Experiment 1: Gateway und SpEL

Das erste Experiment beginnt beim Gateway. Der Gateway Actuator ist in diesem Demo ohne Authentifizierung erreichbar und erlaubt das Anlegen einer Route. In einem Filterwert steckt ein SpEL-Ausdruck. Spring wertet diesen Ausdruck bereits beim Erstellen der Route aus. Dadurch wird ein Runtime Setting geändert, bevor jemand die neue Route überhaupt benutzt. Danach schickt der Gateway bei Archiv-Anfragen einen Debug Header.

[TERMINAL: Jetzt zu Live Demo 1 wechseln. Zuerst die normale /users-Antwort zeigen, dann die Route registrieren und den geänderten Modus und die passwordHash-Felder hervorheben. Danach zu Slide 12 zurückkehren.]

Quelle: https://spring.io/security/cve-2025-41243/

## Slide 12: Warum erscheinen Password Hashes?

Wir sind zurück bei der Antwort von /users. Links sehen wir den normalen Modus: allgemeine Felder, aber keine Password Hashes. Rechts sehen wir den Debug-Modus. Der Gateway liest das geänderte Setting und fügt X-Debug-Trace hinzu. Der Archive Service vertraut diesem Header und liefert mehr Felder aus. Die SpEL-Schwachstelle und diese Vertrauensentscheidung wirken hier zusammen. Das Ergebnis ist nicht nur eine veränderte Konfiguration, sondern ein Datenabfluss, den wir im nächsten Experiment verwenden können.

## Slide 13: Experiment 2: Weak Password

Aus dem ersten Experiment haben wir einen BCrypt Hash. Ein Hash ist nicht direkt das Passwort. Aber eines der Test-Passwörter ist schwach und steht in einer kurzen Wordlist. Nach einem lokalen Vergleich finden wir dieses Passwort. Damit können wir uns am Archiv mit Basic Auth anmelden und die Notizen dieses Benutzers lesen. Eine Notiz gibt den Hinweis auf die Document Search. Wichtig ist: Dieser Schritt ist keine eigene CVE. Er zeigt, dass eine Hash-Leak durch ein schwaches Passwort viel größere Folgen haben kann.

[TERMINAL: Jetzt Live Demo 2 zeigen. Wordlist-Vergleich und anschließend GET /notes mit Basic Auth. Die gefundene Notiz hervorheben. Danach zu Slide 14 zurückkehren.]

## Slide 14: Experiment 3: SQL Injection

Im dritten Experiment betrachten wir die Document Search. Der Client sendet einen department-Wert. Der Archive Service gibt ihn an den MariaDB Filter Converter von Spring AI weiter. In der verwendeten Version wird dieser Wert in einen SQL-Teil eingebaut, ohne dass ein enthaltenes Anführungszeichen korrekt behandelt wird. So verändert der Input die WHERE-Bedingung und die Suche liefert zusätzliche Dokumente. Ich möchte hier vor allem den Datenfluss zeigen: Wo entsteht der String, wo wird er umgewandelt, und wo beeinflusst er die Datenbank-Abfrage?

[TERMINAL: Jetzt Live Demo 3 zeigen. Erst einen normalen Suchaufruf, dann den geänderten Filter. Die unterschiedliche Zahl und Art der Dokumente zeigen. Danach zu Slide 15 zurückkehren.]

Quelle: https://spring.io/security/cve-2026-22730/

## Slide 15: Experiment 4: Login Timing und OTP

Der letzte Teil läuft im separaten command-control Service. Der Login Controller nutzt UserDetailsService und PasswordEncoder direkt. Er verwendet den konfigurierten AuthenticationManager nicht. Wenn ein Benutzer nicht existiert, kommt die Antwort schnell. Bei einem vorhandenen Benutzer läuft BCrypt, und die Antwort dauert länger. Das kann den Benutzernamen verraten. Außerdem hat der OTP-Endpunkt nur vier Ziffern und keine Rate Limit. Viele Versuche können in kurzer Zeit einen gültigen Code finden. Dieser Code liefert einen JWT für den letzten Endpunkt. Eine wichtige Einschränkung des Demo-Codes: Der OTP-Endpunkt verlangt keinen Nachweis eines erfolgreichen Login. Timing und Passwort sind für diesen Schritt technisch nicht zwingend nötig.

[TERMINAL: Jetzt Live Demo 4 zeigen. Timing-Messwerte vergleichen, OTP-Fortschritt, gültigen JWT und die letzte Antwort zeigen. Danach zu Slide 16 zurückkehren.]

Verwandte Spring Security Advisory: https://spring.io/security/cve-2025-22234/

## Slide 16: Was die Experimente gezeigt haben

Hier sehen wir die ganze Untersuchung noch einmal. Der Gateway-Fehler verändert ein Setting. Dadurch erscheinen Password Hashes in der Antwort. Ein schwaches Passwort ermöglicht den Zugriff auf private Notizen. Danach zeigt die Suche wegen SQL Injection zusätzliche Dokumente. Im separaten Service sehen wir schließlich Login Timing und einen OTP-Endpunkt ohne Rate Limit. Der End-to-End-Test erreicht alle vier Marker. Dabei möchte ich eine Grenze klar benennen: Der aktuelle OTP-Endpunkt ist unabhängig von einem erfolgreichen Login. Deshalb ist die Geschichte eine Folge von Experimenten, aber nicht an jeder Stelle eine technisch erzwungene Kette.

## Slide 17: Was ich daraus mitnehme

Für mich bleiben vier praktische Punkte. Erstens müssen wir wissen, welche Versionen von Dependencies und Runtime wirklich laufen, und Updates einplanen. Zweitens sollten Management Endpoints nur dort erreichbar sein, wo sie gebraucht werden. Drittens sollten wir Datenflüsse bis zum Ende prüfen: Ein Header oder ein Input-Wert kann an einer späteren Stelle viel mehr bewirken als erwartet. Viertens ist Authentication besonders empfindlich. Standard-Flows und Rate Limits sind wichtig, auch wenn ein eigener Controller zunächst einfacher aussieht. Das sind allgemeine Engineering-Fragen; diese Demo macht keine Aussage über die Sicherheit unserer konkreten Anwendungen.

## Slide 18: Fragen?

Vielen Dank. Ich freue mich auf eure Fragen. Wir können über die Architektur, die einzelnen Experimente oder über die möglichen Schutzmaßnahmen sprechen. Wenn ihr möchtet, kann ich auch noch einmal einen bestimmten Terminal-Schritt zeigen.
