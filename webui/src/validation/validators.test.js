import { describe, expect, it } from 'vitest'
import { maxFileSize, requirePdf, requireText } from './validators.js'

describe('requirePdf', () => {
  it('lehnt Nicht-PDF-Dateien ab', () => {
    const txt = new File(['hallo'], 'notiz.txt', { type: 'text/plain' })
    expect(requirePdf(txt, 'Datei')).toBe('Datei muss eine PDF-Datei sein.')
  })

  it('akzeptiert .pdf-Dateien mit leerem oder abweichendem MIME-Typ', () => {
    for (const type of ['', 'application/x-pdf', 'application/octet-stream']) {
      const file = new File(['%PDF-1.4'], 'Scan.PDF', { type })
      expect(requirePdf(file, 'Datei')).toBeNull()
    }
  })

  it('lehnt leere Dateien ab', () => {
    const empty = new File([], 'leer.pdf', { type: 'application/pdf' })
    expect(requirePdf(empty, 'Datei')).toBe('Datei darf nicht leer sein.')
  })
})

describe('requireText', () => {
  it('liefert eine Fehlermeldung für nur Leerzeichen', () => {
    expect(requireText(' ', 'Ordnername')).toBe('Ordnername darf nicht leer sein.')
  })

  it('liefert eine Fehlermeldung für leeren String, null und undefined', () => {
    expect(requireText('', 'Ordnername')).not.toBeNull()
    expect(requireText(null, 'Ordnername')).not.toBeNull()
    expect(requireText(undefined, 'Ordnername')).not.toBeNull()
  })
})

describe('maxFileSize', () => {
  it('akzeptiert Dateien bis zur Grenze', () => {
    expect(maxFileSize({ size: 50 * 1024 * 1024 }, 'Die Datei')).toBeNull()
    expect(maxFileSize(null, 'Die Datei')).toBeNull()
  })

  it('lehnt größere Dateien mit verständlicher Meldung ab', () => {
    expect(maxFileSize({ size: 50 * 1024 * 1024 + 1 }, 'Die Datei')).toBe('Die Datei ist zu groß (höchstens 50 MB).')
  })
})
