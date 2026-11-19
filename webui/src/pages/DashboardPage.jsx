import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import DocumentUploadForm from '../components/DocumentUploadForm.jsx'
import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import FolderActions from '../components/FolderActions.jsx'
import FolderForm from '../components/FolderForm.jsx'
import FolderTree from '../components/FolderTree.jsx'
import Modal from '../components/Modal.jsx'
import { deleteDocument, getDocuments, searchDocuments, uploadDocument } from '../api/documentApi.js'
import { createFolder, deleteFolder, getFolders, updateFolder } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { useDialog } from '../hooks/useDialog.js'
import { OCR_POLL_INTERVAL_MS, useOcrPolling } from '../hooks/useOcrPolling.js'
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

  // Lädt die Gesamtliste und, bei aktiver Suche, auch die angezeigten Treffer neu.
  const reloadVisible = useCallback(() => {
    reloadDocuments()
    if (searching) reloadSearch()
  }, [reloadDocuments, reloadSearch, searching])

  const [uploadNotice, setUploadNotice] = useState(null)
  const [documentToDelete, setDocumentToDelete] = useState(null)

  function askDeleteDocument(document) {
    setDocumentToDelete(document)
    dialog.open('deleteDocument')
  }

  const handleDeleteDocument = () =>
    dialog.run(async () => {
      await deleteDocument(documentToDelete.id)
      reloadVisible()
    })

  const handleUploadDocument = ({ file, folderId }) =>
    dialog.run(async () => {
      await uploadDocument(file, folderId)
      setUploadNotice(file.name)
      reloadVisible()
    })

  // Solange eine OCR läuft, regelmäßig neu laden, damit der Statuswechsel ohne F5 sichtbar wird.
  const pendingIds = [...(documents ?? []), ...(searchReady ? searchResult.hits : [])]
    .filter((doc) => doc.status === 'OCR_PENDING')
    .map((doc) => doc.id)
  const hasPendingOcr = pendingIds.length > 0
  const { gaveUp: pollingGaveUp } = useOcrPolling({
    active: hasPendingOcr,
    busy: documentsLoading || (searching && searchLoading),
    failed: Boolean(documentsError || (searching && searchError)),
    reload: reloadVisible,
    // Wartet ein anderes Dokument als vorher, beginnt das Zeitbudget neu.
    resetKey: [...new Set(pendingIds)].sort().join(','),
  })

  // Der Suchindex wird erst kurz nach OCR_DONE befüllt: Treffer danach einmal nachladen.
  const wasPending = useRef(false)
  useEffect(() => {
    const finished = wasPending.current && !hasPendingOcr
    wasPending.current = hasPendingOcr
    if (!finished || !searching) return undefined
    const timer = setTimeout(reloadSearch, OCR_POLL_INTERVAL_MS)
    return () => clearTimeout(timer)
  }, [hasPendingOcr, searching, reloadSearch])

  const path = selectedFolder ? folderPath(allFolders, selectedFolder.id) : []
  const pendingCount = (documents ?? []).filter((doc) => doc.status === 'OCR_PENDING').length

  function renderDocumentList() {
    if (searching) {
      // Treffer bleiben sichtbar, auch während sie im Hintergrund neu geladen werden.
      if (!searchReady) {
        if (searchError && !searchLoading) return null
        return <p className="empty">Suche läuft…</p>
      }
      return (
        <DocumentTable
          documents={visibleDocuments}
          folderNames={folderNames}
          onDelete={askDeleteDocument}
          emptyMessage={`Keine Treffer für ${query}`}
          emptyHint="Prüfe die Schreibweise oder versuche ein anderes Wort. Neue Dokumente sind erst durchsuchbar, wenn ihr Text erkannt wurde."
        />
      )
    }
    if ((documentsLoading || foldersLoading) && !documents) return <p className="empty">Dokumente werden geladen…</p>
    const hasAnyDocument = (documents ?? []).length > 0
    return (
      <DocumentTable
        documents={visibleDocuments}
        folderNames={folderNames}
        onDelete={askDeleteDocument}
        emptyMessage={hasAnyDocument ? 'In diesem Ordner liegen noch keine Dokumente.' : 'Noch keine Dokumente.'}
        emptyHint={hasAnyDocument ? null : 'Lade dein erstes PDF über „PDF hochladen“ hoch, um loszulegen.'}
      />
    )
  }

  const countLabel = searching
    ? searchReady
      ? `${visibleDocuments.length} Treffer für „${query}“`
      : ''
    : `${visibleDocuments.length} ${visibleDocuments.length === 1 ? 'Dokument' : 'Dokumente'}`

  return (
    <div className="layout">
      <FolderTree folders={allFolders} selectedId={folderParam} />
      <main className="content">
        <ErrorBanner error={documentsError} />
        <ErrorBanner error={foldersError} />
        {searching && <ErrorBanner error={searchError} />}
        {uploadNotice && (
          <div role="status" className="banner banner-success">
            <p className="banner-message">
              „{uploadNotice}“ wurde hochgeladen. Der Text wird jetzt erkannt, danach ist das Dokument über die
              Suche auffindbar.
            </p>
            <button
              type="button"
              className="banner-close"
              aria-label="Hinweis schließen"
              onClick={() => setUploadNotice(null)}
            >
              ✕
            </button>
          </div>
        )}
        {pendingCount > 0 && (
          <p role="status" className="banner banner-info">
            {pendingCount === 1
              ? 'Bei 1 Dokument wird gerade der Text erkannt.'
              : `Bei ${pendingCount} Dokumenten wird gerade der Text erkannt.`}{' '}
            {pollingGaveUp
              ? 'Das dauert ungewöhnlich lange. Die Liste aktualisiert sich nicht mehr von selbst, lade die Seite neu, um den Stand zu prüfen.'
              : 'Die Liste aktualisiert sich automatisch.'}
          </p>
        )}

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
              PDF hochladen
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
                placeholder="Nach Wörtern im Text oder Dateinamen suchen…"
                aria-label="Dokumente durchsuchen"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
              <button type="submit" className="btn">
                Suchen
              </button>
              {searching && (
                <button type="button" className="btn" onClick={handleResetSearch}>
                  Suche zurücksetzen
                </button>
              )}
            </form>
            <span className="muted small">{countLabel}</span>
          </div>
          {renderDocumentList()}
        </div>
      </main>

      {dialog.current === 'createDocument' && (
        <Modal title="PDF hochladen" onClose={dialog.close} busy={dialog.busy}>
          <ErrorBanner error={dialog.error} />
          <DocumentUploadForm
            folders={allFolders}
            initialFolderId={selectedFolder?.id ?? ''}
            onSubmit={handleUploadDocument}
            busy={dialog.busy}
          />
        </Modal>
      )}
      {dialog.current === 'createFolder' && (
        <Modal title="Neuer Ordner" onClose={dialog.close} busy={dialog.busy}>
          <ErrorBanner error={dialog.error} />
          <FolderForm
            folders={allFolders}
            initial={{ name: '', parentId: selectedFolder?.id ?? '' }}
            onSubmit={handleCreateFolder}
            busy={dialog.busy}
          />
        </Modal>
      )}
      {dialog.current === 'editFolder' && selectedFolder && (
        <Modal title="Ordner bearbeiten" onClose={dialog.close} busy={dialog.busy}>
          <ErrorBanner error={dialog.error} />
          <FolderForm
            key={selectedFolder.id}
            folders={allFolders}
            excludeIds={folderAndDescendantIds(allFolders, selectedFolder.id)}
            initial={{ name: selectedFolder.name, parentId: selectedFolder.parentId ?? '' }}
            submitLabel="Speichern"
            onSubmit={handleUpdateFolder}
            busy={dialog.busy}
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
          busy={dialog.busy}
        />
      )}
      {dialog.current === 'deleteDocument' && documentToDelete && (
        <ConfirmDialog
          title="Dokument löschen"
          message={`Dokument „${documentToDelete.filename}“ und alle Kommentare wirklich löschen?`}
          error={dialog.error}
          onConfirm={handleDeleteDocument}
          onCancel={dialog.close}
          busy={dialog.busy}
        />
      )}
    </div>
  )
}
