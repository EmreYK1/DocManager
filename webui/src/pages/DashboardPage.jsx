import { useCallback, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import DocumentCreateForm from '../components/DocumentCreateForm.jsx'
import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import FolderActions from '../components/FolderActions.jsx'
import FolderForm from '../components/FolderForm.jsx'
import FolderTree from '../components/FolderTree.jsx'
import Modal from '../components/Modal.jsx'
import { createDocument, getDocuments, searchDocuments } from '../api/documentApi.js'
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
  // `searchInput` ist der Text im Feld, `query` der abgeschickte Suchbegriff.
  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState('')
  const dialog = useDialog()

  // Ergebnis trägt den Suchbegriff mit, damit Treffer einer älteren Suche nie angezeigt werden.
  const loadSearch = useCallback(
    () =>
      query
        ? searchDocuments(query).then((hits) => ({ query, hits }))
        : Promise.resolve(null),
    [query],
  )
  const {
    data: searchResult,
    loading: searchLoading,
    error: searchError,
    reload: reloadSearch,
  } = useApiResource(loadSearch)

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

  const searching = query !== ''
  const searchReady = searching && searchResult?.query === query
  const sourceDocuments = searching ? (searchReady ? searchResult.hits : []) : (documents ?? [])

  // Ordnerfilter gilt auch für Suchtreffer.
  const visibleDocuments = useMemo(
    () => filterDocuments(sourceDocuments, allFolders, folderParam, ''),
    [sourceDocuments, allFolders, folderParam],
  )

  const handleSearch = (event) => {
    event.preventDefault()
    const next = searchInput.trim()
    if (next === query && next !== '') reloadSearch()
    setQuery(next)
  }

  const handleResetSearch = () => {
    setSearchInput('')
    setQuery('')
  }

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
        {searching && <ErrorBanner error={searchError} />}

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
            <form className="search-form" role="search" onSubmit={handleSearch}>
              <input
                type="search"
                className="search"
                placeholder="Volltextsuche in Dokumenten…"
                aria-label="Dokumente durchsuchen"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
              <button type="submit" className="btn btn-primary">
                Suchen
              </button>
              {searching && (
                <button type="button" className="btn" onClick={handleResetSearch}>
                  Suche zurücksetzen
                </button>
              )}
            </form>
            <span className="muted small">{visibleDocuments.length} Dokument(e)</span>
          </div>
          {searching ? (
            searchError ? null : !searchReady || searchLoading ? (
              <p className="empty">Suche läuft…</p>
            ) : (
              <DocumentTable
                documents={visibleDocuments}
                folderNames={folderNames}
                emptyMessage={`Keine Treffer für ${query}`}
              />
            )
          ) : (documentsLoading || foldersLoading) && !documents ? (
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
