import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import FolderForm from '../components/FolderForm.jsx'
import { getDocuments } from '../api/documentApi.js'
import { createFolder, getFolders } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { buildFolderNameMap } from '../utils/folderNames.js'

export default function DashboardPage() {
  const { data: documents, loading: documentsLoading, error: documentsError } = useApiResource(getDocuments)
  const {
    data: folders,
    loading: foldersLoading,
    error: foldersError,
    reload: reloadFolders,
  } = useApiResource(getFolders)

  const folderNames = buildFolderNameMap(folders ?? [])

  async function handleCreateFolder(folder) {
    await createFolder(folder)
    reloadFolders()
  }

  return (
    <main>
      <h1>Dokumente</h1>
      {(documentsLoading || foldersLoading) && <p>Lade…</p>}
      <ErrorBanner error={documentsError} />
      <ErrorBanner error={foldersError} />
      {documents && <DocumentTable documents={documents} folderNames={folderNames} />}

      <h2>Neuer Ordner</h2>
      <FolderForm folders={folders ?? []} onSubmit={handleCreateFolder} />
    </main>
  )
}
