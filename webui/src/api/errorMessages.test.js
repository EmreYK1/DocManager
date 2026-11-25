import { describe, expect, it } from 'vitest'
import { errorMessage } from './errorMessages.ts'

describe('errorMessage', () => {
  it('zeigt bei Status 400 die konkrete Meldung des Servers', () => {
    expect(errorMessage({ status: 400, message: 'Nur PDF-Dateien sind erlaubt' })).toBe('Nur PDF-Dateien sind erlaubt')
  })

  it('zeigt keine fremden oder englischen Meldungen bei Status 400', () => {
    expect(errorMessage({ status: 400, message: 'Bad Request' })).toMatch(/^Die Eingabe ist ungültig/)
    expect(errorMessage({ status: 400, message: 'Ungültiger Request-Body' })).toMatch(/^Die Eingabe ist ungültig/)
  })

  it('meldet einen allgemeinen Fehler bei Status 500 oder ohne Status', () => {
    expect(errorMessage({ status: 500 })).toMatch(/schiefgelaufen/)
    expect(errorMessage(undefined)).toMatch(/schiefgelaufen/)
  })
})
