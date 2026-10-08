import { folderAndDescendantIds } from './folderTree.js'

export const NO_FOLDER = 'none'

// Filtert nach Ordner (inkl. Unterordner, 'none' = ohne Ordner) und Dateiname.
export function filterDocuments(documents, folders, folderParam, query) {
  let result = documents
  if (folderParam === NO_FOLDER) {
    result = result.filter((doc) => !doc.folderId)
  } else if (folders.some((folder) => folder.id === folderParam)) {
    const ids = folderAndDescendantIds(folders, folderParam)
    result = result.filter((doc) => ids.has(doc.folderId))
  }
  const text = query.trim().toLowerCase()
  return text ? result.filter((doc) => doc.filename.toLowerCase().includes(text)) : result
}
