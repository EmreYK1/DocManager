import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import TextField from './TextField.jsx'
import { useTextField } from '../hooks/useTextField.js'

export default function DocumentEditForm({ document, folders = [], onSave, onCancel, busy = false }) {
  // Werte beim Öffnen merken: Lädt die Seite im Hintergrund neu, zählt nur, was hier geändert wurde.
  const [initial] = useState(() => ({
    filename: document.filename,
    folderId: document.folderId ?? '',
  }))

  const filename = useTextField('Dateiname', initial.filename)
  const [folderId, setFolderId] = useState(initial.folderId)

  function handleSubmit(event) {
    event.preventDefault()
    if (filename.error) return

    const changes = {}
    if (filename.value.trim() !== initial.filename) {
      changes.filename = filename.value.trim()
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
