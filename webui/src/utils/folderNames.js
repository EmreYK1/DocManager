export function buildFolderNameMap(folders) {
  const map = new Map()
  for (const folder of folders) {
    map.set(folder.id, folder.name)
  }
  return map
}
