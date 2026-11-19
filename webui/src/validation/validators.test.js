import { describe, expect, it } from 'vitest'
import { maxFileSize, nonNegativeInteger, requirePdf, requireText } from './validators.js'

describe('requirePdf', () => {
  const pdf = new File(['%PDF-1.4'], 'HelloWorld.pdf', { type: 'application/pdf' })

  it('liefert null für eine PDF-Datei', () => {
    expect(requirePdf(pdf, 'Datei')).toBeNull()
  })

  it('verlangt eine ausgewählte Datei', () => {
    expect(requirePdf(null, 'Datei')).toBe('Datei muss ausgewählt werden.')
    expect(requirePdf(undefined, 'Datei')).not.toBeNull()
  })

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

  it('lehnt Dateien ohne PDF-Typ und ohne .pdf-Endung ab', () => {
    const file = new File(['x'], 'bild.png', { type: '' })
    expect(requirePdf(file, 'Datei')).toBe('Datei muss eine PDF-Datei sein.')
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

describe('maxFileSize', () => {
  it('akzeptiert Dateien bis zur Grenze', () => {
    expect(maxFileSize({ size: 50 * 1024 * 1024 }, 'Die Datei')).toBeNull()
    expect(maxFileSize(null, 'Die Datei')).toBeNull()
  })

  it('lehnt größere Dateien mit verständlicher Meldung ab', () => {
    expect(maxFileSize({ size: 50 * 1024 * 1024 + 1 }, 'Die Datei')).toBe(
      'Die Datei ist zu groß (höchstens 50 MB).',
    )
  })
})
