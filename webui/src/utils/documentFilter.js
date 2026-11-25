import { folderAndDescendantIds } from './folderTree.js'

export const NO_FOLDER = 'none'

// Filtert nach Ordner (inkl. Unterordner, 'none' = ohne Ordner).
export function filterDocuments(documents, folders, folderParam) {
  if (folderParam === NO_FOLDER) {
    return documents.filter((doc) => !doc.folderId)
  }
  if (folders.some((folder) => folder.id === folderParam)) {
    const ids = folderAndDescendantIds(folders, folderParam)
    return documents.filter((doc) => ids.has(doc.folderId))
  }
  return documents
}
