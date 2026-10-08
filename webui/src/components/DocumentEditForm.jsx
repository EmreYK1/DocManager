import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import TextField from './TextField.jsx'
import { DOCUMENT_STATUSES } from '../constants/documentStatus.js'
import { useTextField } from '../hooks/useTextField.js'

export default function DocumentEditForm({ document, folders = [], onSave, onCancel }) {
  const initialFolderId = document.folderId ?? ''

  const filename = useTextField('Dateiname', document.filename)
  const [status, setStatus] = useState(document.status)
  const [folderId, setFolderId] = useState(initialFolderId)

  function handleSubmit(event) {
    event.preventDefault()
    if (filename.error) return

    const changes = {}
    if (filename.value.trim() !== document.filename) {
      changes.filename = filename.value.trim()
    }
    if (status !== document.status) {
      changes.status = status
    }
    if (folderId !== initialFolderId) {
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
              {value}
            </option>
          ))}
        </select>
      </label>
      <FolderSelect
        label="Ordner"
        value={folderId}
        onChange={setFolderId}
        folders={folders}
        emptyLabel="– kein Ordner –"
      />
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn" onClick={onCancel}>
            Abbrechen
          </button>
        )}
        <button type="submit" className="btn btn-primary" disabled={Boolean(filename.error)}>
          Speichern
        </button>
      </div>
    </form>
  )
}
