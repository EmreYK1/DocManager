import { useState } from 'react'
import { DOCUMENT_STATUSES } from '../constants/documentStatus.js'

export default function DocumentEditForm({ document, folders = [], onSave }) {
  const initialFolderId = document.folderId ?? ''

  const [filename, setFilename] = useState(document.filename)
  const [status, setStatus] = useState(document.status)
  const [folderId, setFolderId] = useState(initialFolderId)

  function handleSubmit(event) {
    event.preventDefault()

    const changes = {}
    if (filename !== document.filename) {
      changes.filename = filename
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
    <form onSubmit={handleSubmit}>
      <label>
        Dateiname
        <input value={filename} onChange={(event) => setFilename(event.target.value)} />
      </label>
      <label>
        Status
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          {DOCUMENT_STATUSES.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
      </label>
      <label>
        Ordner
        <select value={folderId} onChange={(event) => setFolderId(event.target.value)}>
          <option value="">– kein Ordner –</option>
          {folders.map((folder) => (
            <option key={folder.id} value={folder.id}>
              {folder.name}
            </option>
          ))}
        </select>
      </label>
      <button type="submit">Speichern</button>
    </form>
  )
}
