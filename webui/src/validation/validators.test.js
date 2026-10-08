import { describe, expect, it } from 'vitest'
import { nonNegativeInteger, requireText } from './validators.js'

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

describe('nonNegativeInteger', () => {
  it('akzeptiert ganze Zahlen ab 0', () => {
    expect(nonNegativeInteger('0', 'Größe')).toBeNull()
    expect(nonNegativeInteger(' 2048 ', 'Größe')).toBeNull()
  })

  it('lehnt leere, negative und nicht ganze Werte ab', () => {
    for (const value of ['', '  ', '-5', '1.5', 'abc']) {
      expect(nonNegativeInteger(value, 'Größe')).toBe('Größe muss eine ganze Zahl ab 0 sein.')
    }
  })
})
