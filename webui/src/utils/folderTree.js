export function buildFolderTree(folders) {
  const nodes = new Map(folders.map((folder) => [folder.id, { ...folder, children: [] }]))
  const roots = []
  for (const node of nodes.values()) {
    const parent = node.parentId ? nodes.get(node.parentId) : null
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  }
  const byName = (a, b) => a.name.localeCompare(b.name, 'de')
  const sortRecursive = (list) => {
    list.sort(byName)
    list.forEach((node) => sortRecursive(node.children))
  }
  sortRecursive(roots)
  return roots
}

// IDs des Ordners selbst und aller Unterordner
export function folderAndDescendantIds(folders, folderId) {
  const ids = new Set([folderId])
  let grew = true
  while (grew) {
    grew = false
    for (const folder of folders) {
      if (folder.parentId && ids.has(folder.parentId) && !ids.has(folder.id)) {
        ids.add(folder.id)
        grew = true
      }
    }
  }
  return ids
}

export function folderPath(folders, folderId) {
  const byId = new Map(folders.map((folder) => [folder.id, folder]))
  const path = []
  let current = byId.get(folderId)
  while (current && !path.includes(current)) {
    path.unshift(current)
    current = current.parentId ? byId.get(current.parentId) : null
  }
  return path
}
