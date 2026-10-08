import { describe, expect, it } from 'vitest'
import { buildFolderTree, folderAndDescendantIds, folderPath } from './folderTree.js'

const folders = [
  { id: 'b', name: 'Verträge', parentId: null },
  { id: 'a', name: 'Rechnungen', parentId: null },
  { id: 'a1', name: '2026', parentId: 'a' },
  { id: 'a11', name: 'Q1', parentId: 'a1' },
]

describe('folderTree', () => {
  it('baut einen nach Namen sortierten Baum', () => {
    const tree = buildFolderTree(folders)
    expect(tree.map((n) => n.name)).toEqual(['Rechnungen', 'Verträge'])
    expect(tree[0].children[0].children[0].name).toBe('Q1')
  })

  it('liefert einen Ordner samt allen Unterordnern', () => {
    expect([...folderAndDescendantIds(folders, 'a')].sort()).toEqual(['a', 'a1', 'a11'])
  })

  it('liefert den Pfad von der Wurzel bis zum Ordner', () => {
    expect(folderPath(folders, 'a11').map((f) => f.name)).toEqual(['Rechnungen', '2026', 'Q1'])
  })
})
