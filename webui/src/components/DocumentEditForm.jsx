import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import TextField from './TextField.jsx'
import { DOCUMENT_STATUSES, statusInfo } from '../constants/documentStatus.js'
import { useTextField } from '../hooks/useTextField.js'

export default function DocumentEditForm({ document, folders = [], onSave, onCancel, busy = false }) {
  // Werte beim Öffnen merken. Lädt die Seite im Hintergrund neu (z. B. weil die Texterkennung
  // fertig wurde), darf das Speichern den neuen Status nicht mit dem alten überschreiben.
  const [initial] = useState(() => ({
    filename: document.filename,
    status: document.status,
    folderId: document.folderId ?? '',
  }))

  const filename = useTextField('Dateiname', initial.filename)
  const [status, setStatus] = useState(initial.status)
  const [folderId, setFolderId] = useState(initial.folderId)

  function handleSubmit(event) {
    event.preventDefault()
    if (filename.error) return

    const changes = {}
    if (filename.value.trim() !== initial.filename) {
      changes.filename = filename.value.trim()
    }
    if (status !== initial.status) {
      changes.status = status
    }
    if (folderId !== initial.folderId) {
      if (folderId === '') {
        changes.clearFolder = true
      } else {
        changes.folderId = folderId
      }
    }

    onSave(changes)
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      <TextField label="Dateiname" field={filename} />
      <label className="field">
        Status
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          {DOCUMENT_STATUSES.map((value) => (
            <option key={value} value={value}>
              {statusInfo(value).label}
            </option>
          ))}
        </select>
      </label>
      <p className="form-hint">Der Status wird normalerweise automatisch gesetzt. Ändere ihn nur, wenn nötig.</p>
      <FolderSelect
        label="Ordner"
        value={folderId}
        onChange={setFolderId}
        folders={folders}
        emptyLabel="– kein Ordner –"
      />
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn" onClick={onCancel} disabled={busy}>
            Abbrechen
          </button>
        )}
        <button type="submit" className="btn btn-primary" disabled={Boolean(filename.error) || busy}>
          Speichern
        </button>
      </div>
    </form>
  )
}
