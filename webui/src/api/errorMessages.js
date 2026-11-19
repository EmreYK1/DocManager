const MESSAGES_BY_STATUS = {
  0: 'Keine Verbindung zum Server',
  400: 'Eingabe ungültig',
  404: 'Nicht gefunden',
  409: 'Wird noch verwendet und kann nicht gelöscht werden',
  413: 'Datei zu groß',
}

const DEFAULT_MESSAGE = 'Serverfehler, bitte später erneut versuchen'

export function errorMessage(error) {
  return MESSAGES_BY_STATUS[error?.status] ?? DEFAULT_MESSAGE
}
