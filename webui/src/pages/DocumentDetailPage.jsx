import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import CommentForm from '../components/CommentForm.jsx'
import CommentList from '../components/CommentList.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import DocumentEditForm from '../components/DocumentEditForm.jsx'
import DocumentMetadata from '../components/DocumentMetadata.jsx'
import ErrorBanner from '../components/ErrorBanner.jsx'
import Modal from '../components/Modal.jsx'
import RecognizedText from '../components/RecognizedText.jsx'
import { deleteDocument, getDocument, updateDocument } from '../api/documentApi.ts'
import { createComment, deleteComment, getComments, updateComment } from '../api/commentApi.ts'
import { getFolders } from '../api/folderApi.ts'
import { DocumentStatus } from '../constants/documentStatus.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { useDialog } from '../hooks/useDialog.js'
import { useOcrPolling } from '../hooks/useOcrPolling.js'
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
  const {
    data: comments,
    loading: commentsLoading,
    error: commentsError,
    reload: reloadComments,
  } = useApiResource(loadComments)
  const { data: folders } = useApiResource(getFolders)

  // Während der Texterkennung neu laden, damit Status und erkannter Text ohne F5 erscheinen.
  useOcrPolling({
    active: document?.status === DocumentStatus.OCR_PENDING,
    busy: documentLoading,
    failed: Boolean(documentError),
    reload: reloadDocument,
  })

  const dialog = useDialog()
  const [commentToDelete, setCommentToDelete] = useState(null)
  const [commentError, setCommentError] = useState(null)

  const folderNames = buildFolderNameMap(folders ?? [])

  // Kommentar-Aktionen laufen außerhalb von Dialogen; der Fehler erscheint auf der Seite.
  async function runCommentAction(action) {
    setCommentError(null)
    try {
      await action()
      reloadComments()
      return true
    } catch (err) {
      setCommentError(err)
      return false
    }
  }

  const handleSave = (changes) =>
    dialog.run(async () => {
      await updateDocument(id, changes)
      reloadDocument()
    })

  const handleDelete = () =>
    dialog.run(async () => {
      await deleteDocument(id)
      navigate('/')
    })

  const handleCommentCreate = (data) => runCommentAction(() => createComment(id, data))

  const handleCommentUpdate = (commentId, content) => runCommentAction(() => updateComment(id, commentId, content))

  const handleCommentDelete = () =>
    dialog.run(async () => {
      await deleteComment(id, commentToDelete.id)
      reloadComments()
    })

  function askDeleteComment(comment) {
    setCommentToDelete(comment)
    dialog.open('deleteComment')
  }

  return (
    <main className="content content-narrow">
      <p className="breadcrumb">
        <Link to="/">← Zurück zur Übersicht</Link>
      </p>
      {(documentLoading || commentsLoading) && !document && <p className="empty">Dokument wird geladen…</p>}
      <ErrorBanner error={documentError} />
      <ErrorBanner error={commentsError} />

      {document && (
        <>
          <div className="page-header">
            <h1>{document.filename}</h1>
            <div className="toolbar">
              <button type="button" className="btn btn-primary" onClick={() => dialog.open('edit')}>
                Bearbeiten
              </button>
              <button type="button" className="btn btn-danger-ghost" onClick={() => dialog.open('delete')}>
                Dokument löschen
              </button>
            </div>
          </div>
          <div className="card card-padded">
            <DocumentMetadata document={document} folderName={folderNames.get(document.folderId) ?? '–'} />
          </div>

          <h2 className="section-title">Erkannter Text</h2>
          <div className="card card-padded">
            <RecognizedText status={document.status} text={document.ocrText} />
          </div>
        </>
      )}

      <h2 className="section-title">Kommentare</h2>
      <ErrorBanner error={commentError} />
      <div className="card card-padded">
        {comments && <CommentList comments={comments} onUpdate={handleCommentUpdate} onDelete={askDeleteComment} />}
      </div>
      <h3 className="section-title">Neuer Kommentar</h3>
      <div className="card card-padded">
        <CommentForm onSubmit={handleCommentCreate} />
      </div>

      {dialog.current === 'edit' && document && (
        <Modal title="Dokument bearbeiten" onClose={dialog.close} busy={dialog.busy}>
          <ErrorBanner error={dialog.error} />
          <DocumentEditForm
            document={document}
            folders={folders ?? []}
            onSave={handleSave}
            onCancel={dialog.close}
            busy={dialog.busy}
          />
        </Modal>
      )}
      {dialog.current === 'delete' && document && (
        <ConfirmDialog
          title="Dokument löschen"
          message={`Dokument „${document.filename}“ und alle Kommentare wirklich löschen?`}
          error={dialog.error}
          onConfirm={handleDelete}
          onCancel={dialog.close}
          busy={dialog.busy}
        />
      )}
      {dialog.current === 'deleteComment' && commentToDelete && (
        <ConfirmDialog
          title="Kommentar löschen"
          message="Diesen Kommentar wirklich löschen?"
          error={dialog.error}
          onConfirm={handleCommentDelete}
          onCancel={dialog.close}
          busy={dialog.busy}
        />
      )}
    </main>
  )
}
