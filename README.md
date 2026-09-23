# DocManager

Dokumentenverwaltungssystem mit REST API, JPA/PostgreSQL und Ordner-Hierarchie.

## Starten (Docker)

```bash
docker compose up --build
```

- REST API: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (Datenbank: `docmanager`, User: `docmanager`, Passwort: `docmanager`)

Stoppen:

```bash
docker compose down
```

## Lokal starten (ohne Docker)

Voraussetzungen: JDK 25, Maven, laufende PostgreSQL-Instanz

```bash
mvn package
java -jar document-service/target/document-service-*.jar
```

Die Datenbank-Verbindung kann über Umgebungsvariablen gesetzt werden:

| Variable      | Standard                                    |
|---------------|---------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/docmanager` |
| `DB_USER`     | `docmanager`                                |
| `DB_PASSWORD` | `docmanager`                                |

## Tests

```bash
mvn test
```

## Endpoints

### Dokumente

| Methode | Pfad              | Beschreibung              |
|---------|-------------------|---------------------------|
| POST    | `/documents`      | Dokument anlegen          |
| GET     | `/documents`      | Alle Dokumente abrufen    |
| GET     | `/documents/{id}` | Einzelnes Dokument abrufen|
| PUT     | `/documents/{id}` | Dokument aktualisieren    |
| DELETE  | `/documents/{id}` | Dokument löschen          |

**POST / PUT Body:**
```json
{
  "filename": "bericht.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 2048,
  "folderId": "uuid-des-ordners"
}
```

### Ordner

| Methode | Pfad                      | Beschreibung                        |
|---------|---------------------------|-------------------------------------|
| POST    | `/folders`                | Ordner anlegen                      |
| GET     | `/folders`                | Alle Ordner abrufen                 |
| GET     | `/folders/{id}`           | Einzelnen Ordner abrufen            |
| GET     | `/folders/{id}/children`  | Unterordner eines Ordners abrufen   |
| PUT     | `/folders/{id}`           | Ordner aktualisieren                |
| DELETE  | `/folders/{id}`           | Ordner löschen                      |

**POST / PUT Body:**
```json
{
  "name": "Projektdokumente",
  "parentId": "uuid-des-elternordners"
}
```

`parentId` ist optional – fehlt es, wird ein Root-Ordner angelegt.

## Projektstruktur

```
DocManager/
├── docker-compose.yml
├── pom.xml                          # Root POM (Multi-Modul)
└── document-service/
    ├── Dockerfile
    └── src/main/java/com/docmanager/document/
        ├── controller/              # REST-Endpoints
        ├── dto/                     # Request- und Response-DTOs
        ├── entity/                  # JPA-Entities
        ├── mapper/                  # Entity ↔ DTO Mapping
        ├── repository/              # Spring Data Repositories
        └── service/                 # Business-Logik
```
