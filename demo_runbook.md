# Live Demo — Ablauf für die Präsentation

Die vier Demo-Teile laufen gegen das **lokale, absichtlich verwundbare** Spring-System. Das Script hält nach jeder Etappe an und übernimmt die Ergebnisse in die nächste Etappe. So müssen die vorhandenen Einzel-Scripts frühere Schritte nicht noch einmal sichtbar ausführen. Python läuft in einem Docker-Container und muss unter Windows nicht lokal installiert sein.

## Vor dem Vortrag

Im Terminal im Projektordner:

```bash
docker compose down -v
docker compose up -d --build --wait
docker compose --profile demo build demo-runner
```

Teste zuerst den vollständigen Ablauf ohne Pausen:

```bash
docker compose --profile demo run --rm demo-runner python solve/solve_all.py
```

Der Test verändert den Runtime-Zustand des Gateway. Setze ihn danach für die Präsentation zurück:

```bash
docker compose restart edge-gateway
```

Starte anschließend das Präsentations-Script in einem **zweiten Terminal**:

```bash
docker compose --profile demo run --rm demo-runner
```

Das Script wartet vor jedem Demo-Teil auf Enter. Der Stack muss für eine saubere Vorher/Nachher-Darstellung frisch gestartet sein; nach einem früheren Durchlauf bleibt die Gateway-Einstellung bis zum Neustart verändert.

## Während des Vortrags

| Nach Slide | Im Terminal zeigen | Danach |
|---|---|---|
| 11 | Normale `/users`-Antwort; SpEL-Route; `debug-trace` und `passwordHash` in der neuen Antwort | Zurück zu Slide 12 |
| 13 | Wordlist-Vergleich; gefundenes Test-Passwort; `/notes` mit Basic Auth | Zurück zu Slide 14 |
| 14 | Normale Document Search; geänderter Filter; zusätzliche Dokumente | Zurück zu Slide 15 |
| 15 | Login-Timing-Samples; OTP-Fortschritt; JWT und letzte Antwort | Zurück zu Slide 16 |

## Hinweise

- Für die Demo nur den lokalen Test-Stack verwenden. Die im Repo gesetzten Passwörter und Flags sind absichtliche Testwerte.
- Der OTP-Schritt erzeugt viele lokale Requests und kann je nach Rechner länger dauern. Für eine kurze Version nach dem Timing-Vergleich zu Slide 16 wechseln und das vorbereitete Ergebnis beschreiben.
- Im aktuellen Code verlangt `/verify-otp` keinen erfolgreichen `/login`. Diesen Punkt auf Slide 15 und 16 offen benennen.
- Wenn ein Schritt fehlschlägt: zum nächsten Slide zurückkehren, den Mechanismus erklären und den Live-Teil nicht mehrfach ungeprüft starten.
