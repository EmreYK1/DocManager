import { describe, expect, it } from 'vitest'
import { buildFolderNameMap } from './folderNames.js'

describe('buildFolderNameMap', () => {
  it('ordnet mehreren Ordnern ihren Namen über die ID zu', () => {
    const folders = [
      { id: 'a1', name: 'Rechnungen' },
      { id: 'b2', name: 'Verträge' },
    ]

    const map = buildFolderNameMap(folders)

    expect(map.get('a1')).toBe('Rechnungen')
    expect(map.get('b2')).toBe('Verträge')
  })

  it('liefert undefined für eine unbekannte ID', () => {
    const map = buildFolderNameMap([{ id: 'a1', name: 'Rechnungen' }])

    expect(map.get('unbekannt')).toBeUndefined()
  })

  it('liefert eine leere Map ohne Ordner', () => {
    const map = buildFolderNameMap([])

    expect(map.size).toBe(0)
  })
})
