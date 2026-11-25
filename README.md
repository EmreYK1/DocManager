# DocManager

[![GitHub](https://img.shields.io/badge/GitHub-EmreYK1%2FDocManager-181717?logo=github)](https://github.com/EmreYK1/DocManager)

DocManager ist eine Webanwendung, mit der man PDF-Dokumente hochladen, in Ordnern ablegen und kommentieren kann. Nach dem Upload liest ein Hintergrunddienst den Text aus dem PDF. Danach findet man das Dokument über Wörter aus dem Inhalt oder dem Dateinamen.

Das Projekt ist im Rahmen unseres Studiums entstanden. Das Backend ist in Java mit Spring Boot geschrieben, die Oberfläche in React. Alles läuft in Docker.

## Was die Anwendung kann

- PDFs hochladen (höchstens 50 MB), auf Wunsch direkt in einen Ordner
- Text aus den PDFs automatisch erkennen lassen (Deutsch und Englisch)
- Dokumente über den Inhalt und den Dateinamen suchen. Kleine Tippfehler sind kein Problem: "Helo" findet "Hello", und "helloworld" findet `HelloWorld.pdf`
- Ordner anlegen, umbenennen, verschieben und löschen (ein Ordner muss dafür leer sein)
- Dokumente umbenennen, verschieben und löschen
- Den erkannten Text auf der Detailseite lesen
- Kommentare zu einem Dokument schreiben, ändern und löschen

## Starten

Du brauchst Docker Desktop. Wichtig: Docker Desktop braucht mindestens 4 GB Arbeitsspeicher. Mit weniger wird Elasticsearch vom System beendet, und es kommt keine verständliche Fehlermeldung. Wenn `docker compose ps` später `elasticsearch` als `exited (137)` anzeigt, liegt es fast sicher daran. Den Speicher stellt man in Docker Desktop unter Settings, Resources ein. Unter Windows mit WSL 2 geht es auch über die Datei `.wslconfig` mit dem Eintrag `memory=4GB`. Außerdem solltest du etwa 6 GB Platz für die Images haben.

```bash
git clone https://github.com/EmreYK1/DocManager.git
cd DocManager
docker compose up -d --build
```

Für Google Gemini braucht man zusätzlich einen API-Key, siehe [Gemini-API-Key einrichten](#gemini-api-key-einrichten). Alle anderen Funktionen laufen auch ohne Key.

Das `-d` sorgt dafür, dass alles im Hintergrund läuft. Ohne `-d` schreiben alle Container gleichzeitig ins Terminal, und das sind schnell ein paar tausend Zeilen. Beim ersten Mal dauert es einige Minuten, weil die Images gebaut und geladen werden.

Mit `docker compose ps` siehst du, ob alles läuft. Es müssen 10 Container laufen. Das Backend braucht nach dem Start etwa eine Minute, bis es bereit ist. Die Weboberfläche startet erst danach. Wenn du sie zu früh aufrufst, siehst du kurz "502 Bad Gateway".

Wenn alles läuft, öffnest du http://localhost.

Ein paar Befehle, die man oft braucht:

```bash
docker compose ps                        # läuft alles?
docker compose logs -f document-service  # Logs eines Dienstes ansehen
docker compose down                      # alles stoppen, die Daten bleiben
docker compose down -v                   # alles stoppen und alle Daten löschen
docker compose up -d --build webui       # nur die Oberfläche neu bauen
```

### Gemini-API-Key einrichten

Der `genai-worker` braucht einen API-Key für Google Gemini.

1. Unter https://aistudio.google.com/apikey mit einem Google-Konto anmelden und einen API-Key erstellen.
2. Im Projektordner die Vorlage kopieren: `cp .env.example .env`
3. In der `.env` den Key hinter `GEMINI_API_KEY=` eintragen. Mit `GEMINI_MODEL` kann man ein anderes Modell wählen (Standard `gemini-3.8-flash`).
4. `docker compose up -d --build genai-worker` ausführen. Mit `docker compose logs genai-worker` sieht man, ob die Konfiguration erkannt wurde.

Die `.env` steht in der `.gitignore`. Den Key nie in `.env.example`, in `docker-compose.yml` oder in den Code schreiben. Ohne Key läuft der Worker weiter und schreibt eine Fehlermeldung ins Log. Gleichnamige Umgebungsvariablen im Terminal haben Vorrang vor der `.env`. Achtung: `docker compose config` zeigt den Key im Klartext an, also die Ausgabe nicht weitergeben.

### Erste Schritte

Auf http://localhost kannst du mit "PDF hochladen" ein PDF auswählen. Ein Beispiel liegt unter `docs/samples/HelloWorld.pdf`. Nach dem Upload steht das Dokument in der Tabelle mit dem Status "Text wird erkannt...". Nach ein paar Sekunden wechselt der Status von selbst auf "Durchsuchbar". Dann kannst du im Suchfeld nach einem Wort aus dem PDF suchen, zum Beispiel nach "Hello". Ein Klick auf den Dateinamen öffnet die Detailseite mit dem erkannten Text und den Kommentaren.

## Aus welchen Teilen die Anwendung besteht

Beim Start werden 10 Container gestartet:

- `webui` (Port 80): die Oberfläche. Sie wird von nginx ausgeliefert und leitet alle Aufrufe an `/api` an das Backend weiter.
- `document-service`: das Backend mit der REST-API. Es hat keinen eigenen Port nach außen, man erreicht es nur über die Oberfläche unter `http://localhost/api`.
- `ocr-worker`: liest den Text aus den PDFs (mit Ghostscript und Tesseract).
- `indexing-worker`: trägt den erkannten Text in den Suchindex ein.
- `genai-worker`: soll Zusammenfassungen mit Google Gemini erstellen. Bisher verbindet er sich nur mit RabbitMQ und prüft beim Start, ob die Konfiguration vorhanden ist.
- `db` (Port 5432): PostgreSQL für Dokumente, Ordner, Kommentare und den erkannten Text. Benutzer und Passwort sind beide `docmanager`.
- `minio` (Ports 9000 und 9090): speichert die PDF-Dateien. Die Konsole ist unter http://localhost:9090 erreichbar, Benutzer und Passwort sind `minioadmin`.
- `rabbitmq` (Ports 5672 und 15672): die Nachrichten-Queue zwischen Backend und Workern. Die Verwaltungsseite ist unter http://localhost:15672 erreichbar, Benutzer und Passwort sind `guest`.
- `elasticsearch` (Port 9200): die Suchmaschine.
- `kibana` (Port 5601): eine Oberfläche, mit der man in den Suchindex schauen kann. Die Anwendung braucht sie nicht, wir nutzen sie nur zum Kontrollieren.

Die Daten liegen in Docker-Volumes und bleiben bei `docker compose down` erhalten. Erst `docker compose down -v` löscht sie.

Falls auf deinem Rechner schon ein eigenes PostgreSQL auf Port 5432 läuft, verbindet sich alles, was lokal außerhalb von Docker startet (zum Beispiel das Backend per `java -jar`), mit diesem und nicht mit dem Container. Das haben wir selbst erlebt. Dann hilft es, das lokale PostgreSQL zu stoppen oder die Variable `DB_URL` anzupassen.

## Was nach einem Upload passiert

1. Die Oberfläche schickt das PDF an `POST /api/documents/upload`. Das Backend prüft den Content-Type und ob die Datei mit `%PDF-` beginnt, legt die Datei in MinIO ab und speichert das Dokument mit dem Status `OCR_PENDING`. Sobald das Dokument gespeichert ist, schickt es den OCR-Auftrag an RabbitMQ.
2. Der `ocr-worker` holt das PDF aus MinIO und wandelt es mit Ghostscript in Bilder um. Tesseract liest den Text von jeder Seite. Das Ergebnis, oder bei einem unlesbaren PDF eine Fehlermeldung, schickt er zurück.
3. Das Backend speichert den Text und setzt den Status auf `OCR_DONE` oder `FAILED`. Danach beauftragt es den `indexing-worker`, Dateiname und Text in Elasticsearch zu schreiben. Auch beim Umbenennen und Löschen bekommt der `indexing-worker` einen Auftrag. Nur er schreibt in den Index, und alle Aufträge laufen durch dieselbe Queue.
4. Bei einer Suche fragt das Backend Elasticsearch nach den passenden Dokumenten und holt sie dann aus der Datenbank.
5. Solange ein Dokument auf `OCR_PENDING` steht, lädt die Oberfläche die Liste alle 3 Sekunden neu (höchstens 5 Minuten lang). So sieht man den neuen Status, ohne die Seite neu zu laden.

### Wenn etwas ausfällt

- Schlägt die Verarbeitung einer Nachricht fehl, wird sie viermal versucht (mit 1, 3 und 9 Sekunden Abstand). Danach landet sie in der Queue `docmanager.dead-letter`. Dort kann man sie in der RabbitMQ-Verwaltung ansehen und erneut senden. Ein gescheiterter OCR-Auftrag setzt das Dokument außerdem auf `FAILED`.
- Kommt eine Nachricht doppelt an, schadet das nicht.

In der Oberfläche zeigen wir statt der Statuswerte aus der API einen verständlichen Text an: `OCR_PENDING` heißt dort "Text wird erkannt...", `OCR_DONE` heißt "Durchsuchbar" und `FAILED` heißt "Texterkennung fehlgeschlagen". Die Werte `UPLOADED` und `SUMMARY_DONE` gibt es im Datenmodell auch, sie werden aber im Moment nicht automatisch gesetzt.

## Abnahme-Checkliste

Mit dieser Liste kann man auf einem frischen Stand prüfen, ob alles funktioniert. Du brauchst dafür einen frischen Clone und Docker Desktop mit mindestens 4 GB Speicher. Die Ports 80, 5432, 5672, 5601, 9000, 9090, 9200 und 15672 müssen frei sein. Achtung: Schritt 1 löscht alle DocManager-Daten in Docker auf diesem Rechner.

- [ ] `docker compose down -v` ausführen. Bei einem frischen Clone gibt es nichts zu löschen, das ist in Ordnung.
- [ ] `docker compose build` ausführen. Der Build muss ohne Fehler durchlaufen. Beim ersten Mal dauert das ein paar Minuten.
- [ ] `docker compose up -d` ausführen. Es werden 10 Container erstellt.
- [ ] Ein bis zwei Minuten warten und dann `docker compose ps` ausführen. `db`, `minio`, `rabbitmq`, `elasticsearch` und `document-service` müssen `healthy` sein. `ocr-worker`, `indexing-worker`, `genai-worker`, `webui` und `kibana` müssen laufen (`Up`).
- [ ] http://localhost öffnen. Die Übersicht ist leer und zeigt "Noch keine Dokumente."
- [ ] Auf "PDF hochladen" klicken, `docs/samples/HelloWorld.pdf` auswählen und auf "Hochladen" klicken. Der Dialog schließt sich, und `HelloWorld.pdf` steht mit dem Status "Text wird erkannt..." in der Tabelle.
- [ ] Die Seite nicht neu laden und ein paar Sekunden warten. Der Status wechselt von selbst auf "Durchsuchbar" (in der API `OCR_DONE`).
- [ ] Im Suchfeld "Hello" eingeben und Enter drücken. `HelloWorld.pdf` wird gefunden.
- [ ] "Helo" suchen. `HelloWorld.pdf` wird trotz Tippfehler gefunden.
- [ ] "xyzabc" suchen. Es erscheint "Keine Treffer für xyzabc". Mit "Suche zurücksetzen" sind wieder alle Dokumente zu sehen.
- [ ] Auf `HelloWorld.pdf` klicken. Die Detailseite zeigt unter "Erkannter Text" den Text "Hello World".
- [ ] Das Dokument löschen und danach noch einmal nach "Hello" suchen. Es gibt keinen Treffer mehr.

Wer lieber die Kommandozeile benutzt, kann das Gleiche mit `curl` prüfen:

```bash
# Hochladen, die Antwort enthält die id des Dokuments
curl -F "file=@docs/samples/HelloWorld.pdf;type=application/pdf" http://localhost/api/documents/upload

# Status ansehen, bis dort "OCR_DONE" steht (die id einsetzen)
curl http://localhost/api/documents/<id>

# Suchen, "Helo" geht auch
curl "http://localhost/api/documents/search?q=Hello"

# Eine leere Suche liefert den Status 400
curl -o /dev/null -w "%{http_code}\n" "http://localhost/api/documents/search?q="

# Löschen, danach ist die Suche nach Hello leer
curl -X DELETE http://localhost/api/documents/<id>
curl "http://localhost/api/documents/search?q=Hello"
```

Wenn etwas nicht klappt:

- `elasticsearch` ist `exited (137)` oder startet immer wieder neu: Docker hat zu wenig Arbeitsspeicher, siehe oben.
- Die Oberfläche zeigt "502 Bad Gateway": Das Backend startet noch. Eine Minute warten und neu laden.
- Der Status bleibt auf "Text wird erkannt...": Mit `docker compose logs ocr-worker` nachsehen, ob der Worker läuft und `rabbitmq` gesund ist.
- Die Suche meldet, dass sie nicht verfügbar ist: Dann ist Elasticsearch nicht gesund.
- Ein Port ist schon belegt: Das Programm beenden, das ihn benutzt.

## REST-API

Alle Pfade sind über die Oberfläche unter `/api` erreichbar, zum Beispiel `http://localhost/api/documents`. Das Präfix `/api` entfernt nginx, das Backend selbst kennt nur `/documents`. Fehler haben die Form `{"status": 404, "message": "..."}`. Eine unbekannte ID ergibt `404` (auch ein Kommentar, der zu einem anderen Dokument gehört), eine ungültige Eingabe `400`, ein Ordner, der noch benutzt wird, oder eine gleichzeitige Änderung `409`, und wenn die Suche nicht erreichbar ist, kommt `503`.

### Dokumente

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| POST | `/documents/upload` | PDF hochladen, startet die Texterkennung |
| GET | `/documents/search?q=...` | Volltextsuche |
| GET | `/documents` | alle Dokumente |
| GET | `/documents/{id}` | ein Dokument, mit erkanntem Text |
| PATCH | `/documents/{id}` | Dokument teilweise ändern |
| DELETE | `/documents/{id}` | Dokument löschen (mit Kommentaren, Datei und Index-Eintrag) |

Beim Upload schickt man ein Formular (`multipart/form-data`) mit dem Feld `file` und optional mit `folderId`. Die Antwort ist `201` mit dem neuen Dokument. Ist die Datei kein PDF oder leer, kommt `400`.

```bash
curl -F "file=@docs/samples/HelloWorld.pdf;type=application/pdf" -F "folderId=<uuid>" http://localhost/api/documents/upload
```

Bei der Suche ist der Parameter `q` Pflicht. Fehlt er oder ist er leer, kommt `400`. Es werden höchstens 100 Treffer geliefert, die besten zuerst. Ein neues Dokument kann erst gefunden werden, wenn sein Status `OCR_DONE` ist.

`GET /documents` und die Suche liefern kürzere Einträge als `GET /documents/{id}`: Dort fehlen `ocrText` und `summary`. Das haben wir so gemacht, weil die Oberfläche die Liste während der Texterkennung alle paar Sekunden neu lädt und der Text vieler Dokumente zusammen ziemlich groß werden kann.

```json
{
  "id": "uuid",
  "filename": "HelloWorld.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 593,
  "uploadedAt": "2026-11-19T20:25:00Z",
  "status": "OCR_DONE",
  "folderId": null,
  "objectKey": "...-HelloWorld.pdf",
  "ocrText": "Hello World"
}
```

Beim Ändern mit `PATCH` sind alle Felder optional. Mit `clearFolder: true` nimmt man ein Dokument aus seinem Ordner heraus. Den Status kann man nicht setzen, er ergibt sich aus der Texterkennung.

```json
{
  "filename": "neu.pdf",
  "folderId": "uuid-des-ordners",
  "clearFolder": false
}
```

### Ordner

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| POST | `/folders` | Ordner anlegen |
| GET | `/folders` | alle Ordner |
| GET | `/folders/{id}` | ein Ordner |
| GET | `/folders/{id}/children` | die Unterordner |
| PUT | `/folders/{id}` | umbenennen oder verschieben |
| DELETE | `/folders/{id}` | löschen (nur wenn er leer ist, sonst `409`) |

Anlegen und Ändern benutzen denselben Body. Ein Ordner kann nicht in sich selbst oder in einen seiner Unterordner verschoben werden (`400`). Ohne `parentId` entsteht ein Ordner auf der obersten Ebene. Der `name` ist Pflicht und darf höchstens 255 Zeichen lang sein.

```json
{
  "name": "Projektdokumente",
  "parentId": "uuid-des-elternordners"
}
```

### Kommentare

| Methode | Pfad | Beschreibung |
|---------|------|--------------|
| POST | `/documents/{documentId}/comments` | Kommentar anlegen |
| GET | `/documents/{documentId}/comments` | alle Kommentare eines Dokuments |
| GET | `/documents/{documentId}/comments/{id}` | ein Kommentar |
| PUT | `/documents/{documentId}/comments/{id}` | Inhalt ändern |
| DELETE | `/documents/{documentId}/comments/{id}` | Kommentar löschen |

`author` (höchstens 100 Zeichen) und `content` (höchstens 2000 Zeichen) sind Pflicht. Beim Ändern wird nur `content` verwendet.

```json
{
  "author": "Max",
  "content": "Sieht gut aus"
}
```

## Umgebungsvariablen

In Docker setzt `docker-compose.yml` alle nötigen Werte. Läuft ein Dienst ohne Docker, gelten die Standardwerte, und die zeigen auf `localhost`. Die Zugangsdaten sind reine Entwicklungswerte.

Das Backend (`document-service`) kennt diese Variablen:

- `DB_URL`, `DB_USER`, `DB_PASSWORD`: die Datenbank (Standard `jdbc:postgresql://localhost:5432/docmanager`, `docmanager`, `docmanager`)
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASS`: RabbitMQ (Standard `localhost`, `5672`, `guest`, `guest`)
- `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`: MinIO (Standard `http://localhost:9000`, `minioadmin`, `minioadmin`, `documents`)
- `ELASTICSEARCH_URL`, `ELASTICSEARCH_INDEX`: Elasticsearch (Standard `http://localhost:9200` und `documents`)

Der `ocr-worker` nutzt dieselben Variablen für RabbitMQ und MinIO. Dazu kommen:

- `OCR_LANGUAGE`: die Sprachen für Tesseract (Standard `deu+eng`)
- `OCR_TIMEOUT_SECONDS`: wie lange ein einzelner Aufruf von Ghostscript oder Tesseract höchstens dauern darf (Standard `120`)
- `OCR_GS_COMMAND` und `OCR_TESSERACT_COMMAND`: Name der Programme (Standard `gs` und `tesseract`, unter Windows heißt Ghostscript `gswin64c`)

Der `indexing-worker` braucht die Variablen für RabbitMQ sowie `ELASTICSEARCH_URL` und `ELASTICSEARCH_INDEX`.

Der `genai-worker` braucht die Variablen für RabbitMQ. Dazu nutzt er `GEMINI_API_KEY` (nötig für Gemini) und `GEMINI_MODEL` (Standard `gemini-3.8-flash`), `GEMINI_TIMEOUT` (Standard `60s`) und `GEMINI_MAX_INPUT_CHARS` (Standard `100000`, längerer Text wird gekürzt). Ohne Docker liest er die `.env` nicht, dann müssen die Variablen im Terminal gesetzt sein.

Die Namen von Exchange, Queues und Routing-Keys stehen an einer Stelle: `MessagingTopology` im Modul `messaging-contract`.

Das Datenbankschema verwaltet Flyway (`document-service/src/main/resources/db/migration`). Änderungen kommen als neues Skript dazu. Eine ältere Datenbank ohne Flyway wird beim Start automatisch übernommen.

## Entwickeln und Testen

Man kann Backend und Oberfläche auch ohne den ganzen Docker-Stack starten. Dafür braucht man JDK 25, Maven und Node 22. Die Dienste, die man dabei braucht, lassen sich trotzdem über Docker starten:

```bash
docker compose up -d db rabbitmq minio elasticsearch
```

Backend bauen und starten:

```bash
mvn package
java -jar document-service/target/document-service-*.jar
```

Oberfläche mit dem Dev-Server auf http://localhost:5173. Aufrufe an `/api` gehen dabei an das Backend auf Port 8080:

```bash
cd webui
npm install
npm run dev
```

Tests und Prüfungen:

```bash
mvn test                       # Backend, alle Module (braucht Docker)
mvn spotless:apply             # Java-Code formatieren

cd webui
npm test                       # Oberfläche
npm run lint                   # ESLint und Prettier ("npm run format" formatiert)
npm run typecheck              # TypeScript im Ordner api
```

Die meisten Backend-Tests sind Unit-Tests. `DocumentFlowIntegrationTest` prüft den Weg eines Dokuments durch das echte Backend. PostgreSQL, RabbitMQ und MinIO startet er selbst als Container (Testcontainers), dafür muss nur Docker laufen.

Der Test `GeminiSummaryServiceRealTest` im `genai-worker` ruft die echte Gemini-API auf. Er läuft nur, wenn `GEMINI_API_KEY` im Terminal gesetzt ist, sonst wird er übersprungen. Die anderen Tests simulieren die API.

Der Test `TesseractOcrServiceRealTest` im `ocr-worker` liest das echte `HelloWorld.pdf`. Er läuft nur, wenn Tesseract und Ghostscript installiert sind, sonst wird er übersprungen. Die Tests der Oberfläche benutzen Vitest und Testing Library und mocken die API.

## Technik

- Oberfläche: React 18, Vite, React Router, TypeScript für die API-Schicht
- Backend: Java 25, Spring Boot 3.5, Spring Data JPA, Spring AMQP
- Datenbank: PostgreSQL 17, Schema mit Flyway
- Dateien: MinIO
- Nachrichten: RabbitMQ
- Texterkennung: Ghostscript und Tesseract
- Suche: Elasticsearch 8, dazu Kibana zum Nachschauen
- Webserver: nginx
- Tests: JUnit 5, Mockito, Testcontainers, Vitest, Testing Library
- Codestil: Spotless, ESLint, Prettier
- Betrieb: Docker und Docker Compose, CI mit GitHub Actions

## Projektstruktur

```
DocManager/
├── messaging-contract/   Nachrichten und Queues, die sich alle Dienste teilen
├── document-service/     Backend (Spring Boot, REST-API, Upload, Suche)
├── ocr-worker/           liest den Text aus den PDFs
├── indexing-worker/      schreibt den Text in Elasticsearch
├── genai-worker/         Zusammenfassungen mit Google Gemini (im Aufbau)
├── webui/                Oberfläche (React) mit nginx
├── docs/samples/         Beispiel-PDF für Tests und Abnahme
├── docker-compose.yml    startet alle 10 Container
├── .github/workflows/    CI (Stil, Typen und Tests)
└── pom.xml               Maven-Projekt für die fünf Java-Module
```

Im Backend ist der Code nach Aufgaben getrennt: `controller` (REST), `service` (Logik; Interfaces gibt es nur für Zugriffe nach außen wie Speicher und Suche, die Umsetzung liegt in `impl`), `repository`, `entity`, `dto`, `mapper`, `messaging` (RabbitMQ) und `exception`. In der Oberfläche liegen die Seiten in `pages`, die wiederverwendbaren Teile in `components`, die Aufrufe an das Backend in `api` und die Prüfungen für Eingaben in `validation`.

## Was noch fehlt

- Es gibt keine Benutzerverwaltung. Der Autor eines Kommentars ist einfach ein Text, den man eintippt.
- Eine automatische Zusammenfassung gibt es noch nicht. Das Feld `summary` und der Status `SUMMARY_DONE` sind im Datenmodell vorbereitet, werden aber noch nicht gefüllt.
- Die Suche im Text arbeitet mit ganzen Wörtern. "ello" findet also nicht "Hello". Im Dateinamen funktioniert das schon.
- Es werden nur PDFs angenommen.
- Das Ganze ist kein Produktivsystem. Die Zugangsdaten stehen im Klartext in `docker-compose.yml`, und es gibt weder HTTPS noch eine Anmeldung.

## Team

- Emre Can Yüksel (Projektleiter)
- Shez Abbas Soltani
- Abdullah Hakimi
