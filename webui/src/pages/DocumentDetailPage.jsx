import { useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import DocumentMetadata from '../components/DocumentMetadata.jsx'
import DocumentEditForm from '../components/DocumentEditForm.jsx'
import CommentList from '../components/CommentList.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import { deleteDocument, getDocument, updateDocument } from '../api/documentApi.js'
import { getComments } from '../api/commentApi.js'
import { getFolders } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { buildFolderNameMap } from '../utils/folderNames.js'

export default function DocumentDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const loadDocument = useCallback(() => getDocument(id), [id])
  const loadComments = useCallback(() => getComments(id), [id])

  const {
    data: document,
    loading: documentLoading,
    error: documentError,
    reload: reloadDocument,
  } = useApiResource(loadDocument)
  const { data: comments, loading: commentsLoading, error: commentsError } = useApiResource(loadComments)
  const { data: folders } = useApiResource(getFolders)

  const folderNames = buildFolderNameMap(folders ?? [])

  async function handleSave(changes) {
    await updateDocument(id, changes)
    reloadDocument()
  }

  async function handleDelete() {
    if (!window.confirm('Dokument wirklich löschen?')) {
      return
    }
    await deleteDocument(id)
    navigate('/')
  }

  return (
    <main>
      {(documentLoading || commentsLoading) && <p>Lade…</p>}
      <ErrorBanner error={documentError} />
      <ErrorBanner error={commentsError} />

      {document && (
        <>
          <DocumentMetadata document={document} folderName={folderNames.get(document.folderId) ?? '–'} />
          <DocumentEditForm document={document} folders={folders ?? []} onSave={handleSave} />
          <button onClick={handleDelete}>Dokument löschen</button>
        </>
      )}

      <h2>Kommentare</h2>
      {comments && <CommentList comments={comments} />}
    </main>
  )
}
