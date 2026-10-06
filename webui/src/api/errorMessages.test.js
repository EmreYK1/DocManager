import { describe, expect, it } from 'vitest'
import { errorMessage } from './errorMessages.js'

describe('errorMessage', () => {
  it('meldet fehlende Verbindung bei Status 0', () => {
    expect(errorMessage({ status: 0 })).toBe('Keine Verbindung zum Server')
  })

  it('meldet "Nicht gefunden" bei Status 404', () => {
    expect(errorMessage({ status: 404 })).toBe('Nicht gefunden')
  })

  it('meldet einen Serverfehler bei Status 500', () => {
    expect(errorMessage({ status: 500 })).toBe('Serverfehler, bitte später erneut versuchen')
  })

  it('meldet einen Serverfehler, wenn kein Status vorliegt', () => {
    expect(errorMessage(undefined)).toBe('Serverfehler, bitte später erneut versuchen')
  })
})
