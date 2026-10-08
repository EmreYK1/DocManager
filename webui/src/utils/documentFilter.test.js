import { describe, expect, it } from 'vitest'
import { filterDocuments } from './documentFilter.js'

const folders = [
  { id: 'a', name: 'Rechnungen', parentId: null },
  { id: 'a1', name: '2026', parentId: 'a' },
]
const documents = [
  { id: '1', filename: 'Bericht.pdf', folderId: 'a1' },
  { id: '2', filename: 'Vertrag.pdf', folderId: null },
  { id: '3', filename: 'Rechnung.pdf', folderId: 'a' },
]

describe('filterDocuments', () => {
  it('liefert ohne Filter alle Dokumente', () => {
    expect(filterDocuments(documents, folders, null, '')).toHaveLength(3)
  })

  it('enthält beim Ordnerfilter auch Unterordner', () => {
    expect(filterDocuments(documents, folders, 'a', '').map((d) => d.id)).toEqual(['1', '3'])
  })

  it('findet Dokumente ohne Ordner und filtert nach Dateiname ohne Beachtung der Schreibweise', () => {
    expect(filterDocuments(documents, folders, 'none', '').map((d) => d.id)).toEqual(['2'])
    expect(filterDocuments(documents, folders, null, ' BERICHT ').map((d) => d.id)).toEqual(['1'])
  })
})
