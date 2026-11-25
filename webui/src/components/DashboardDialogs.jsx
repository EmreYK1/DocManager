import { deleteDocument, uploadDocument } from '../api/documentApi.ts'
import { createFolder, deleteFolder, updateFolder } from '../api/folderApi.ts'
import { folderAndDescendantIds } from '../utils/folderTree.js'
import ConfirmDialog from './ConfirmDialog.jsx'
import DocumentUploadForm from './DocumentUploadForm.jsx'
import ErrorBanner from './ErrorBanner.jsx'
import FolderForm from './FolderForm.jsx'
import Modal from './Modal.jsx'

// Alle Dialoge der Übersicht samt der Aktionen, die sie auslösen.
export default function DashboardDialogs({
  dialog,
  folders,
  selectedFolder,
  documentToDelete,
  onFoldersChanged,
  onFolderDeleted,
  onDocumentsChanged,
  onUploaded,
}) {
  const runFolderAction = (action) =>
    dialog.run(async () => {
      await action()
      onFoldersChanged()
    })

  const handleUpload = ({ file, folderId }) =>
    dialog.run(async () => {
      await uploadDocument(file, folderId)
      onUploaded(file.name)
      onDocumentsChanged()
    })

  const handleDeleteDocument = () =>
    dialog.run(async () => {
      await deleteDocument(documentToDelete.id)
      onDocumentsChanged()
    })

  const handleDeleteFolder = () =>
    runFolderAction(async () => {
      await deleteFolder(selectedFolder.id)
      onFolderDeleted()
    })

  function formDialog(title, form) {
    return (
      <Modal title={title} onClose={dialog.close} busy={dialog.busy}>
        <ErrorBanner error={dialog.error} />
        {form}
      </Modal>
    )
  }

  function confirmDialog(title, message, onConfirm) {
    return (
      <ConfirmDialog
        title={title}
        message={message}
        error={dialog.error}
        onConfirm={onConfirm}
        onCancel={dialog.close}
        busy={dialog.busy}
      />
    )
  }

  switch (dialog.current) {
    case 'createDocument':
      return formDialog(
        'PDF hochladen',
        <DocumentUploadForm
          folders={folders}
          initialFolderId={selectedFolder?.id ?? ''}
          onSubmit={handleUpload}
          busy={dialog.busy}
        />,
      )
    case 'createFolder':
      return formDialog(
        'Neuer Ordner',
        <FolderForm
          folders={folders}
          initial={{ name: '', parentId: selectedFolder?.id ?? '' }}
          onSubmit={(folder) => runFolderAction(() => createFolder(folder))}
          busy={dialog.busy}
        />,
      )
    case 'editFolder':
      return (
        selectedFolder &&
        formDialog(
          'Ordner bearbeiten',
          <FolderForm
            key={selectedFolder.id}
            folders={folders}
            excludeIds={folderAndDescendantIds(folders, selectedFolder.id)}
            initial={{ name: selectedFolder.name, parentId: selectedFolder.parentId ?? '' }}
            submitLabel="Speichern"
            onSubmit={(folder) => runFolderAction(() => updateFolder(selectedFolder.id, folder))}
            busy={dialog.busy}
          />,
        )
      )
    case 'deleteFolder':
      return (
        selectedFolder &&
        confirmDialog('Ordner löschen', `Ordner „${selectedFolder.name}“ wirklich löschen?`, handleDeleteFolder)
      )
    case 'deleteDocument':
      return (
        documentToDelete &&
        confirmDialog(
          'Dokument löschen',
          `Dokument „${documentToDelete.filename}“ und alle Kommentare wirklich löschen?`,
          handleDeleteDocument,
        )
      )
    default:
      return null
  }
}
