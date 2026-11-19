import { describe, expect, it } from 'vitest'
import { errorMessage } from './errorMessages.js'

describe('errorMessage', () => {
  it('meldet fehlende Verbindung bei Status 0', () => {
    expect(errorMessage({ status: 0 })).toMatch(/^Keine Verbindung zum Server/)
  })

  it('zeigt bei Status 400 die konkrete Meldung des Servers', () => {
    expect(errorMessage({ status: 400, message: 'Nur PDF-Dateien sind erlaubt' })).toBe(
      'Nur PDF-Dateien sind erlaubt',
    )
  })

  it('zeigt keine fremden oder englischen Meldungen bei Status 400', () => {
    expect(errorMessage({ status: 400, message: 'Bad Request' })).toMatch(/^Die Eingabe ist ungültig/)
    expect(errorMessage({ status: 400, message: 'Ungültiger Request-Body' })).toMatch(/^Die Eingabe ist ungültig/)
  })

  it('meldet allgemein ungültige Eingabe bei Status 400 ohne konkrete Meldung', () => {
    expect(errorMessage({ status: 400 })).toMatch(/^Die Eingabe ist ungültig/)
    expect(errorMessage({ status: 400, message: 'Validierungsfehler' })).toMatch(/^Die Eingabe ist ungültig/)
  })

  it('meldet "nicht gefunden" bei Status 404', () => {
    expect(errorMessage({ status: 404 })).toMatch(/nicht gefunden/)
  })

  it('nennt das Größenlimit bei Status 413', () => {
    expect(errorMessage({ status: 413 })).toMatch(/50 MB/)
  })

  it('meldet eine nicht verfügbare Suche bei Status 503', () => {
    expect(errorMessage({ status: 503 })).toMatch(/Suche ist gerade nicht verfügbar/)
  })

  it('meldet einen allgemeinen Fehler bei Status 500 oder ohne Status', () => {
    expect(errorMessage({ status: 500 })).toMatch(/schiefgelaufen/)
    expect(errorMessage(undefined)).toMatch(/schiefgelaufen/)
  })
})
