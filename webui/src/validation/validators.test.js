import { describe, expect, it } from 'vitest'
import { requireText } from './validators.js'

describe('requireText', () => {
  it('liefert eine Fehlermeldung für nur Leerzeichen', () => {
    expect(requireText(' ', 'Ordnername')).toBe('Ordnername darf nicht leer sein.')
  })

  it('liefert eine Fehlermeldung für leeren String, null und undefined', () => {
    expect(requireText('', 'Ordnername')).not.toBeNull()
    expect(requireText(null, 'Ordnername')).not.toBeNull()
    expect(requireText(undefined, 'Ordnername')).not.toBeNull()
  })

  it('liefert null für gültigen Text, auch mit umgebenden Leerzeichen', () => {
    expect(requireText('Steuern', 'Ordnername')).toBeNull()
    expect(requireText('  Steuern ', 'Ordnername')).toBeNull()
  })
})
