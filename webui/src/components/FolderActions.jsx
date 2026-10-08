const DELETE_HINT = 'Nur leere Ordner (ohne Unterordner und Dokumente) können gelöscht werden.'

export default function FolderActions({ canDelete, onEdit, onDelete }) {
  return (
    <div className="folder-actions">
      <button type="button" className="btn btn-small" onClick={onEdit}>
        Ordner bearbeiten
      </button>
      <button
        type="button"
        className="btn btn-small btn-danger-ghost"
        disabled={!canDelete}
        title={canDelete ? '' : DELETE_HINT}
        onClick={onDelete}
      >
        Ordner löschen
      </button>
    </div>
  )
}
