# DocManager

[![GitHub](https://img.shields.io/badge/GitHub-EmreYK1%2FDocManager-181717?logo=github)](https://github.com/EmreYK1/DocManager)

> 👆 Auf das Badge klicken, um zum GitHub-Repository zu gelangen.

Dokumentenverwaltung mit Ordnerstruktur, Status und Kommentaren

Eine webbasierte Anwendung zum Verwalten von Dokumenten. Dokumente werden in beliebig verschachtelten Ordnern abgelegt, mit Metadaten und einem Bearbeitungsstatus versehen und können kommentiert werden. Das Backend stellt eine REST-API mit PostgreSQL bereit, die Weboberfläche ist eine React-Anwendung.

## Überblick

DocManager besteht aus drei Containern: einer PostgreSQL-Datenbank, dem Spring-Boot-Backend und einem nginx, der die React-Oberfläche ausliefert und Anfragen an `/api` an das Backend weiterreicht. Die Oberfläche greift ausschließlich über diese REST-API auf die Daten zu.

Aktuell werden die Metadaten der Dokumente verwaltet, die Dateien selbst werden noch nicht gespeichert (siehe [Einschränkungen](#einschränkungen)). Das Datenmodell ist aber schon auf spätere Erweiterungen wie OCR-Text und Zusammenfassung vorbereitet.

**Hauptfunktionen:**

- Dokumentübersicht mit Status-Badges, Größenanzeige und Suche nach Dateiname
- Ordnerbaum in der Seitenleiste, ein Ordner zeigt auch die Dokumente seiner Unterordner
- Dokumente anlegen, umbenennen, verschieben, Status ändern und löschen
- Ordner anlegen, umbenennen, verschieben und löschen (nur leere Ordner)
- Kommentare zu Dokumenten schreiben, bearbeiten und löschen
- Eingabeprüfung vor dem Absenden, ungültige Formulare lassen sich nicht abschicken
- Verständliche Fehlermeldungen, wenn der Server nicht erreichbar ist oder eine Aktion nicht möglich ist

## Schnellstart

### Voraussetzungen

- Docker Desktop installiert und laufend

### Installation

Repository klonen:

```bash
git clone https://github.com/EmreYK1/DocManager.git
cd DocManager
```

Projekt starten:

```bash
docker compose up --build
```

Beim ersten Start werden alle Container gebaut. Die Tabellen legt das Backend selbst an, eine Konfiguration ist nicht nötig.

Anwendung öffnen:

- Weboberfläche: http://localhost
- REST-API: http://localhost:8080
- PostgreSQL: `localhost:5432` (Datenbank, Benutzer und Passwort jeweils `docmanager`)

### Erste Schritte

1. http://localhost im Browser öffnen
2. Mit „+ Neuer Ordner“ einen Ordner anlegen
3. Mit „+ Neues Dokument“ ein Dokument eintragen. Der aktuell gewählte Ordner ist vorausgewählt
4. Auf den Dateinamen klicken, um die Detailseite zu öffnen
5. Dort Metadaten bearbeiten oder Kommentare schreiben

## Technologie-Stack

- **Frontend:** React 18, Vite, React Router
- **Backend:** Java 25, Spring Boot 3.5, Spring Data JPA
- **Datenbank:** PostgreSQL 17
- **Webserver:** nginx (Auslieferung der Oberfläche und Proxy für `/api`)
- **Tests:** JUnit 5 und Mockito (Backend), Vitest und Testing Library (Frontend)
- **Deployment:** Docker und Docker Compose

## Projektstruktur

```
DocManager/
├── document-service/               # Backend (Spring Boot)
│   ├── src/main/java/com/docmanager/document/
│   │   ├── controller/             # REST-Endpunkte (Dokumente, Ordner, Kommentare)
│   │   ├── service/                # Geschäftslogik, Interfaces und impl/
│   │   ├── repository/             # Spring-Data-Repositories
│   │   ├── entity/                 # JPA-Entities: Document, Folder, Comment
│   │   ├── dto/                    # Request- und Response-Objekte
│   │   ├── mapper/                 # Entity <-> DTO
│   │   ├── exception/              # NotFoundException, globaler Fehler-Handler
│   │   └── DocumentServiceApplication.java
│   ├── src/main/resources/application.properties
│   └── Dockerfile
│
├── webui/                          # Frontend (React)
│   ├── src/
│   │   ├── pages/                  # DashboardPage, DocumentDetailPage
│   │   ├── components/             # Tabelle, Ordnerbaum, Formulare, Dialoge
│   │   │   ├── DocumentTable, DocumentMetadata, StatusBadge
│   │   │   ├── FolderTree, FolderActions, FolderForm, FolderSelect
│   │   │   ├── DocumentCreateForm, DocumentEditForm
│   │   │   ├── CommentList, CommentForm
│   │   │   └── Modal, ConfirmDialog, ErrorBanner, TextField
│   │   ├── hooks/                  # useApiResource, useDialog, useTextField
│   │   ├── api/                    # REST-Aufrufe, HTTP-Client, Fehlermeldungen
│   │   ├── validation/             # Prüffunktionen für Eingaben (reine Funktionen)
│   │   ├── utils/                  # Ordnerbaum, Filter, Datums- und Größenformat
│   │   ├── layout/                 # Seitenrahmen mit Kopfleiste
│   │   ├── constants/              # Dokumentstatus
│   │   └── styles/                 # Stylesheets, nach Bereichen getrennt
│   ├── nginx.conf                  # SPA-Fallback und /api-Proxy
│   └── Dockerfile
│
├── docker-compose.yml              # Datenbank, Backend und Weboberfläche
├── pom.xml                         # Root-POM (Multi-Modul)
└── README.md
```

## Funktionsweise

**Anfragen.** Der Browser schickt Aufrufe an `/api/...`. nginx entfernt das Präfix und leitet sie an das Backend weiter. Dort durchläuft jede Anfrage Controller, Service und Repository, bevor sie die Datenbank erreicht. Die Controller arbeiten mit DTOs, die Entities verlassen das Backend nie.

**Fehler.** Unbekannte IDs liefern `404`. Wird eine Ressource noch verwendet, zum Beispiel ein Ordner mit Inhalt, antwortet das Backend mit `409`. Fehler haben die Form `{"status": 404, "message": "..."}` und werden in der Oberfläche als deutsche Meldung angezeigt.

**Dokumentstatus.** Ein neues Dokument beginnt mit `UPLOADED`. Weitere Werte sind `OCR_PENDING`, `OCR_DONE`, `SUMMARY_DONE` und `FAILED`. Aktuell setzt man den Status von Hand auf der Detailseite.

**Eingabeprüfung.** Die Regeln liegen als reine Funktionen in `webui/src/validation` und werden von allen Formularen genutzt. Pflichtfelder dürfen nicht leer sein, auch nicht nur aus Leerzeichen bestehen. Die Größe eines Dokuments muss eine ganze Zahl ab 0 sein. Solange ein Fehler besteht, ist der Absenden-Button deaktiviert und es wird kein Request geschickt.

**Löschen.** Beim Löschen eines Dokuments werden seine Kommentare mitgelöscht. Ein Ordner lässt sich nur löschen, wenn er weder Unterordner noch Dokumente enthält. Die Oberfläche deaktiviert den Button in diesem Fall.

## REST-API

### Dokumente

| Methode | Pfad              | Beschreibung                      |
|---------|-------------------|-----------------------------------|
| POST    | `/documents`      | Dokument anlegen                  |
| GET     | `/documents`      | Alle Dokumente abrufen            |
| GET     | `/documents/{id}` | Einzelnes Dokument abrufen        |
| PATCH   | `/documents/{id}` | Dokument teilweise ändern         |
| DELETE  | `/documents/{id}` | Dokument samt Kommentaren löschen |

Anlegen:

```json
{
  "filename": "bericht.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 2048,
  "folderId": "uuid-des-ordners"
}
```

Ändern. Alle Felder sind optional, `clearFolder: true` löst das Dokument aus seinem Ordner:

```json
{
  "filename": "neu.pdf",
  "status": "OCR_DONE",
  "folderId": "uuid-des-ordners",
  "clearFolder": false
}
```

### Ordner

| Methode | Pfad                     | Beschreibung                       |
|---------|--------------------------|------------------------------------|
| POST    | `/folders`               | Ordner anlegen                     |
| GET     | `/folders`               | Alle Ordner abrufen                |
| GET     | `/folders/{id}`          | Einzelnen Ordner abrufen           |
| GET     | `/folders/{id}/children` | Unterordner abrufen                |
| PUT     | `/folders/{id}`          | Ordner umbenennen oder verschieben |
| DELETE  | `/folders/{id}`          | Ordner löschen                     |

Anlegen und Ändern verwenden denselben Body. Ohne `parentId` entsteht ein Ordner auf oberster Ebene.

```json
{
  "name": "Projektdokumente",
  "parentId": "uuid-des-elternordners"
}
```

### Kommentare

| Methode | Pfad                                    | Beschreibung                |
|---------|-----------------------------------------|-----------------------------|
| POST    | `/documents/{documentId}/comments`      | Kommentar anlegen           |
| GET     | `/documents/{documentId}/comments`      | Kommentare eines Dokuments  |
| GET     | `/documents/{documentId}/comments/{id}` | Einzelnen Kommentar abrufen |
| PUT     | `/documents/{documentId}/comments/{id}` | Inhalt ändern               |
| DELETE  | `/documents/{documentId}/comments/{id}` | Kommentar löschen           |

Beim Ändern wird nur `content` ausgewertet.

```json
{
  "author": "Max",
  "content": "Sieht gut aus"
}
```

## Entwicklung

Backend und Frontend lassen sich auch ohne Docker starten. Für das Backend werden JDK 25 und Maven gebraucht, für das Frontend Node 22. Die Datenbank kann trotzdem aus Docker kommen:

```bash
docker compose up -d db
```

Backend:

```bash
mvn package
java -jar document-service/target/document-service-*.jar
```

Frontend mit Dev-Server auf http://localhost:5173. Anfragen an `/api` gehen an das Backend auf Port 8080:

```bash
cd webui
npm install
npm run dev
```

Die Datenbankverbindung lässt sich über Umgebungsvariablen anpassen:

| Variable      | Standard                                      |
|---------------|-----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/docmanager` |
| `DB_USER`     | `docmanager`                                  |
| `DB_PASSWORD` | `docmanager`                                  |

### Tests

```bash
# Backend (die Datenbank wird gemockt)
mvn test

# Frontend
cd webui
npm test
```

## Nützliche Befehle

```bash
# Alle Services im Hintergrund starten
docker compose up -d

# Logs anzeigen
docker compose logs -f

# Nur Backend-Logs
docker compose logs -f document-service

# Services stoppen
docker compose down

# Services stoppen und Volumes löschen (löscht die Datenbank!)
docker compose down -v

# Datenbank-Zugriff
docker compose exec db psql -U docmanager -d docmanager

# Nach Änderungen am Frontend nur die Oberfläche neu bauen
docker compose up -d --build webui
```

## Einschränkungen

- **Kein Datei-Upload.** Beim Anlegen werden Dateiname, Typ und Größe von Hand eingetragen, die Datei selbst wird nicht abgelegt. Die Felder für OCR-Text und Zusammenfassung sind im Datenmodell vorhanden, werden aber noch nicht befüllt.
- **Keine Validierung im Backend.** Die Eingaben werden nur in der Oberfläche geprüft, die API akzeptiert auch leere Namen.
- **Keine Benutzerverwaltung.** Der Autor eines Kommentars ist ein frei eingegebener Text.
- **Kein Produktions-Setup.** Die Zugangsdaten der Datenbank stehen im Klartext in `docker-compose.yml`.

## Projektteam

- Emre Can Yüksel (Projektleiter)
- Shez Abbas Soltani
- Abdullah Hakimi
