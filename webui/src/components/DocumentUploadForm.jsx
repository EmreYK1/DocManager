import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import { formatSize } from '../utils/formatSize.js'
import { maxFileSize, requirePdf } from '../validation/validators.js'

export default function DocumentUploadForm({ folders = [], onSubmit, initialFolderId = '', busy = false }) {
  const [file, setFile] = useState(null)
  const [folderId, setFolderId] = useState(initialFolderId)
  const error = requirePdf(file, 'Datei') ?? maxFileSize(file, 'Die Datei')
  // Fehler erst zeigen, wenn eine Datei gewählt wurde (ein frisches Formular ist nicht sofort rot).
  const visibleError = file ? error : null

  function handleSubmit(event) {
    event.preventDefault()
    if (error || busy) return
    onSubmit({ file, folderId: folderId || null })
  }

  return (
    <form onSubmit={handleSubmit} className="form" aria-busy={busy}>
      <p className="form-hint">
        Wähle eine PDF-Datei (höchstens 50 MB). Nach dem Hochladen wird der Text automatisch erkannt. Danach findest du
        das Dokument auch über die Suche nach Wörtern aus dem Inhalt.
      </p>
      <label className="field">
        PDF-Datei
        <input
          type="file"
          accept="application/pdf,.pdf"
          disabled={busy}
          onChange={(event) => setFile(event.target.files[0] ?? null)}
        />
      </label>
      {file && !error && (
        <p className="form-hint">
          Ausgewählt: <strong>{file.name}</strong> ({formatSize(file.size)})
        </p>
      )}
      {visibleError && (
        <p role="alert" className="field-error">
          {visibleError}
        </p>
      )}
      <FolderSelect
        label="Ordner (optional)"
        value={folderId}
        onChange={setFolderId}
        folders={folders}
        emptyLabel="– kein Ordner –"
      />
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={Boolean(error) || busy}>
          {busy ? 'Wird hochgeladen…' : 'Hochladen'}
        </button>
      </div>
    </form>
  )
}
