import { useCallback, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import DocumentMetadata from '../components/DocumentMetadata.jsx'
import DocumentEditForm from '../components/DocumentEditForm.jsx'
import CommentList from '../components/CommentList.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import { deleteDocument, getDocument, updateDocument } from '../api/documentApi.js'
import { getComments, createComment } from '../api/commentApi.js'
import { getFolders } from '../api/folderApi.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { buildFolderNameMap } from '../utils/folderNames.js'
import CommentForm from '../components/CommentForm.jsx'

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
  const { data: comments, loading: commentsLoading, error: commentsError, reload: reloadComments } = useApiResource(loadComments)
  const { data: folders } = useApiResource(getFolders)
  const [commentSubmitError, setCommentSubmitError] = useState(null)

  const folderNames = buildFolderNameMap(folders ?? [])

  async function handleSave(changes) {
    await updateDocument(id, changes)
    reloadDocument()
  }

  async function handleComment(data) {
    setCommentSubmitError(null)
    try {
      await createComment(id, data)
      reloadComments()
    } catch (err) {
      setCommentSubmitError(err)
      throw err
    }
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
      <ErrorBanner error={commentSubmitError} />
      <CommentForm onSubmit={handleComment} />
      {comments && <CommentList comments={comments} />}
    </main>
  )
}
