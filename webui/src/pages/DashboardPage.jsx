import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import DocumentCreateForm from '../components/DocumentCreateForm.jsx'
import FolderForm from '../components/FolderForm.jsx'
import { createDocument, getDocuments } from '../api/documentApi.js'
import { createFolder, getFolders } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { buildFolderNameMap } from '../utils/folderNames.js'

export default function DashboardPage() {
  const {
    data: documents,
    loading: documentsLoading,
    error: documentsError,
    reload: reloadDocuments,
  } = useApiResource(getDocuments)
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

  async function handleCreateDocument(document) {
    await createDocument(document)
    reloadDocuments()
  }

  return (
    <main>
      <h1>Dokumente</h1>
      {(documentsLoading || foldersLoading) && <p>Lade…</p>}
      <ErrorBanner error={documentsError} />
      <ErrorBanner error={foldersError} />
      {documents && <DocumentTable documents={documents} folderNames={folderNames} />}

      <h2>Neues Dokument</h2>
      <DocumentCreateForm folders={folders ?? []} onSubmit={handleCreateDocument} />

      <h2>Neuer Ordner</h2>
      <FolderForm folders={folders ?? []} onSubmit={handleCreateFolder} />
    </main>
  )
}
