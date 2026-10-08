import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import DocumentCreateForm from '../components/DocumentCreateForm.jsx'
import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import FolderActions from '../components/FolderActions.jsx'
import FolderForm from '../components/FolderForm.jsx'
import FolderTree from '../components/FolderTree.jsx'
import Modal from '../components/Modal.jsx'
import { createDocument, getDocuments } from '../api/documentApi.js'
import { createFolder, deleteFolder, getFolders, updateFolder } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { useDialog } from '../hooks/useDialog.js'
import { filterDocuments, NO_FOLDER } from '../utils/documentFilter.js'
import { buildFolderNameMap } from '../utils/folderNames.js'
import { folderAndDescendantIds, folderPath } from '../utils/folderTree.js'

function pageTitle(selectedFolder, folderParam) {
  if (selectedFolder) return selectedFolder.name
  return folderParam === NO_FOLDER ? 'Ohne Ordner' : 'Alle Dokumente'
}

export default function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const folderParam = searchParams.get('folder')
  const [search, setSearch] = useState('')
  const dialog = useDialog()

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

  const allFolders = useMemo(() => folders ?? [], [folders])
  const folderNames = buildFolderNameMap(allFolders)
  const selectedFolder = allFolders.find((folder) => folder.id === folderParam) ?? null

  // Löschen ist nur sicher, wenn beide Listen geladen sind und der Ordner leer ist.
  const canDeleteFolder =
    Boolean(selectedFolder && documents) &&
    !allFolders.some((folder) => folder.parentId === selectedFolder.id) &&
    !documents.some((doc) => doc.folderId === selectedFolder.id)

  const visibleDocuments = useMemo(
    () => filterDocuments(documents ?? [], allFolders, folderParam, search),
    [documents, allFolders, folderParam, search],
  )

  const handleCreateFolder = (folder) =>
    dialog.run(async () => {
      await createFolder(folder)
      reloadFolders()
    })

  const handleUpdateFolder = (folder) =>
    dialog.run(async () => {
      await updateFolder(selectedFolder.id, folder)
      reloadFolders()
    })

  const handleDeleteFolder = () =>
    dialog.run(async () => {
      await deleteFolder(selectedFolder.id)
      setSearchParams({})
      reloadFolders()
    })

  const handleCreateDocument = (document) =>
    dialog.run(async () => {
      await createDocument(document)
      reloadDocuments()
    })

  const path = selectedFolder ? folderPath(allFolders, selectedFolder.id) : []

  return (
    <div className="layout">
      <FolderTree folders={allFolders} selectedId={folderParam} />
      <main className="content">
        <ErrorBanner error={documentsError} />
        <ErrorBanner error={foldersError} />

        <div className="page-header">
          <div>
            {path.length > 0 && (
              <p className="breadcrumb">
                <Link to="/">Alle Dokumente</Link>
                {path.slice(0, -1).map((folder) => (
                  <span key={folder.id}>
                    {' / '}
                    <Link to={`/?folder=${folder.id}`}>{folder.name}</Link>
                  </span>
                ))}
              </p>
            )}
            <h1>{pageTitle(selectedFolder, folderParam)}</h1>
          </div>
          <div className="toolbar">
            <button type="button" className="btn btn-primary" onClick={() => dialog.open('createDocument')}>
              + Neues Dokument
            </button>
            <button type="button" className="btn" onClick={() => dialog.open('createFolder')}>
              + Neuer Ordner
            </button>
          </div>
        </div>

        {selectedFolder && (
          <FolderActions
            canDelete={canDeleteFolder}
            onEdit={() => dialog.open('editFolder')}
            onDelete={() => dialog.open('deleteFolder')}
          />
        )}

        <div className="card">
          <div className="card-toolbar">
            <input
              type="search"
              className="search"
              placeholder="Dokumente nach Dateiname filtern…"
              aria-label="Dokumente filtern"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <span className="muted small">{visibleDocuments.length} Dokument(e)</span>
          </div>
          {(documentsLoading || foldersLoading) && !documents ? (
            <p className="empty">Lade…</p>
          ) : (
            <DocumentTable documents={visibleDocuments} folderNames={folderNames} />
          )}
        </div>
      </main>

      {dialog.current === 'createDocument' && (
        <Modal title="Neues Dokument" onClose={dialog.close}>
          <ErrorBanner error={dialog.error} />
          <DocumentCreateForm
            folders={allFolders}
            initialFolderId={selectedFolder?.id ?? ''}
            onSubmit={handleCreateDocument}
            onCancel={dialog.close}
          />
        </Modal>
      )}
      {dialog.current === 'createFolder' && (
        <Modal title="Neuer Ordner" onClose={dialog.close}>
          <ErrorBanner error={dialog.error} />
          <FolderForm
            folders={allFolders}
            initial={{ name: '', parentId: selectedFolder?.id ?? '' }}
            onSubmit={handleCreateFolder}
            onCancel={dialog.close}
          />
        </Modal>
      )}
      {dialog.current === 'editFolder' && selectedFolder && (
        <Modal title="Ordner bearbeiten" onClose={dialog.close}>
          <ErrorBanner error={dialog.error} />
          <FolderForm
            key={selectedFolder.id}
            folders={allFolders}
            excludeIds={folderAndDescendantIds(allFolders, selectedFolder.id)}
            initial={{ name: selectedFolder.name, parentId: selectedFolder.parentId ?? '' }}
            submitLabel="Speichern"
            onSubmit={handleUpdateFolder}
            onCancel={dialog.close}
          />
        </Modal>
      )}
      {dialog.current === 'deleteFolder' && selectedFolder && (
        <ConfirmDialog
          title="Ordner löschen"
          message={`Ordner „${selectedFolder.name}“ wirklich löschen?`}
          error={dialog.error}
          onConfirm={handleDeleteFolder}
          onCancel={dialog.close}
        />
      )}
    </div>
  )
}
