import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import TextField from './TextField.jsx'
import { useTextField } from '../hooks/useTextField.js'
import { resetAfterSubmit } from '../utils/afterSubmit.js'
import { nonNegativeInteger } from '../validation/validators.js'

export default function DocumentCreateForm({ folders = [], onSubmit, initialFolderId = '', onCancel }) {
  const filename = useTextField('Dateiname')
  const contentType = useTextField('Inhaltstyp', 'application/pdf')
  const sizeBytes = useTextField('Größe', '0', nonNegativeInteger)
  const [folderId, setFolderId] = useState(initialFolderId)
  const hasError = Boolean(filename.error || contentType.error || sizeBytes.error)

  function handleSubmit(event) {
    event.preventDefault()
    if (hasError) return
    const result = onSubmit({
      filename: filename.value.trim(),
      contentType: contentType.value.trim(),
      sizeBytes: Number(sizeBytes.value),
      folderId: folderId || null,
    })
    resetAfterSubmit(result, () => {
      filename.reset()
      sizeBytes.reset()
      setFolderId(initialFolderId)
    })
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      <TextField label="Dateiname" field={filename} autoFocus />
      <TextField label="Inhaltstyp" field={contentType} />
      <TextField label="Größe (Bytes)" field={sizeBytes} />
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
        <button type="submit" className="btn btn-primary" disabled={hasError}>
          Dokument anlegen
        </button>
      </div>
    </form>
  )
}
