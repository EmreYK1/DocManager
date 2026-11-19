const MESSAGES_BY_STATUS = {
  0: 'Keine Verbindung zum Server. Bitte prüfe deine Verbindung und versuche es gleich noch einmal.',
  400: 'Die Eingabe ist ungültig. Bitte prüfe deine Angaben.',
  404: 'Das wurde nicht gefunden. Vielleicht wurde es inzwischen gelöscht.',
  409: 'Das wird noch verwendet und kann deshalb nicht gelöscht werden.',
  413: 'Die Datei ist zu groß. Erlaubt sind höchstens 50 MB.',
  503: 'Die Suche ist gerade nicht verfügbar. Bitte versuche es in einem Moment noch einmal.',
}

const DEFAULT_MESSAGE = 'Da ist etwas schiefgelaufen. Bitte versuche es später noch einmal.'

// Konkrete deutsche Meldungen des Backends, die bei 400 direkt angezeigt werden dürfen.
// Alles andere (z. B. „Bad Request“ von einem Proxy) bekommt den allgemeinen Text.
const SERVER_MESSAGES_400 = new Set([
  'Nur PDF-Dateien sind erlaubt',
  'Die Datei darf nicht leer sein',
  'Der Suchbegriff darf nicht leer sein',
])

export function errorMessage(error) {
  if (error?.status === 400 && SERVER_MESSAGES_400.has(error.message)) {
    return error.message
  }
  return MESSAGES_BY_STATUS[error?.status] ?? DEFAULT_MESSAGE
}
