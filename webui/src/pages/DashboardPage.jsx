import { useCallback, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import DashboardDialogs from '../components/DashboardDialogs.jsx'
import DocumentTable from '../components/DocumentTable.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import FolderActions from '../components/FolderActions.jsx'
import FolderTree from '../components/FolderTree.jsx'
import { getDocuments } from '../api/documentApi.ts'
import { getFolders } from '../api/folderApi.ts'
import { DocumentStatus } from '../constants/documentStatus.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { useDialog } from '../hooks/useDialog.js'
import { useDocumentSearch } from '../hooks/useDocumentSearch.js'
import { usePendingOcrRefresh } from '../hooks/usePendingOcrRefresh.js'
import { filterDocuments, NO_FOLDER } from '../utils/documentFilter.js'
import { buildFolderNameMap } from '../utils/folderNames.js'
import { folderPath } from '../utils/folderTree.js'

function pageTitle(selectedFolder, folderParam) {
  if (selectedFolder) return selectedFolder.name
  return folderParam === NO_FOLDER ? 'Ohne Ordner' : 'Alle Dokumente'
}

export default function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const folderParam = searchParams.get('folder')
  const search = useDocumentSearch()
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

  // Ordnerfilter gilt auch für Suchtreffer.
  const sourceDocuments = search.active ? search.hits : documents
  const visibleDocuments = useMemo(
    () => filterDocuments(sourceDocuments ?? [], allFolders, folderParam),
    [sourceDocuments, allFolders, folderParam],
  )

  const handleSearch = (event) => {
    event.preventDefault()
    search.submit()
  }

  // Lädt die Gesamtliste und, bei aktiver Suche, auch die angezeigten Treffer neu.
  const { active: searching, reload: reloadSearch } = search
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

  const { gaveUp: pollingGaveUp } = usePendingOcrRefresh({
    documents: [...(documents ?? []), ...search.hits],
    search,
    busy: documentsLoading || search.loading,
    failed: Boolean(documentsError || search.error),
    reload: reloadVisible,
  })

  const path = selectedFolder ? folderPath(allFolders, selectedFolder.id) : []
  const pendingCount = (documents ?? []).filter((doc) => doc.status === DocumentStatus.OCR_PENDING).length

  function renderDocumentList() {
    if (search.active) {
      // Treffer bleiben sichtbar, auch während sie im Hintergrund neu geladen werden.
      if (!search.ready) {
        if (search.error && !search.loading) return null
        return <p className="empty">Suche läuft…</p>
      }
      return (
        <DocumentTable
          documents={visibleDocuments}
          folderNames={folderNames}
          onDelete={askDeleteDocument}
          emptyMessage={`Keine Treffer für ${search.query}`}
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

  const countLabel = search.active
    ? search.ready
      ? `${visibleDocuments.length} Treffer für „${search.query}“`
      : ''
    : `${visibleDocuments.length} ${visibleDocuments.length === 1 ? 'Dokument' : 'Dokumente'}`

  return (
    <div className="layout">
      <FolderTree folders={allFolders} selectedId={folderParam} />
      <main className="content">
        <ErrorBanner error={documentsError} />
        <ErrorBanner error={foldersError} />
        <ErrorBanner error={search.error} />
        {uploadNotice && (
          <div role="status" className="banner banner-success">
            <p className="banner-message">
              „{uploadNotice}“ wurde hochgeladen. Der Text wird jetzt erkannt, danach ist das Dokument über die Suche
              auffindbar.
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
                value={search.input}
                onChange={(event) => search.setInput(event.target.value)}
              />
              <button type="submit" className="btn">
                Suchen
              </button>
              {search.active && (
                <button type="button" className="btn" onClick={search.reset}>
                  Suche zurücksetzen
                </button>
              )}
            </form>
            <span className="muted small">{countLabel}</span>
          </div>
          {renderDocumentList()}
        </div>
      </main>

      <DashboardDialogs
        dialog={dialog}
        folders={allFolders}
        selectedFolder={selectedFolder}
        documentToDelete={documentToDelete}
        onFoldersChanged={reloadFolders}
        onFolderDeleted={() => setSearchParams({})}
        onDocumentsChanged={reloadVisible}
        onUploaded={setUploadNotice}
      />
    </div>
  )
}
