import { useState } from 'react'
import { requireText } from '../validation/validators.js'

export default function DocumentCreateForm({ folders = [], onSubmit }) {
  const [filename, setFilename] = useState('')
  const [contentType, setContentType] = useState('application/pdf')
  const [sizeBytes, setSizeBytes] = useState('0')
  const [folderId, setFolderId] = useState('')
  const [touched, setTouched] = useState(false)
  const filenameError = requireText(filename, 'Dateiname')
  const contentTypeError = requireText(contentType, 'Inhaltstyp')
  const sizeNumber = Number(sizeBytes)
  const sizeError =
    sizeBytes.trim() === '' || !Number.isInteger(sizeNumber) || sizeNumber < 0
      ? 'Größe muss eine ganze Zahl ab 0 sein.'
      : null
  const hasError = Boolean(filenameError || contentTypeError || sizeError)

  function handleSubmit(event) {
    event.preventDefault()
    if (hasError) return
    onSubmit({
      filename: filename.trim(),
      contentType: contentType.trim(),
      sizeBytes: sizeNumber,
      folderId: folderId || null,
    })
    setFilename('')
    setSizeBytes('0')
    setFolderId('')
    setTouched(false)
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        Dateiname
        <input
          value={filename}
          onChange={(event) => {
            setFilename(event.target.value)
            setTouched(true)
          }}
        />
      </label>
      {touched && filenameError && <p role="alert">{filenameError}</p>}
      <label>
        Inhaltstyp
        <input value={contentType} onChange={(event) => setContentType(event.target.value)} />
      </label>
      {contentTypeError && <p role="alert">{contentTypeError}</p>}
      <label>
        Größe (Bytes)
        <input value={sizeBytes} onChange={(event) => setSizeBytes(event.target.value)} />
      </label>
      {sizeError && <p role="alert">{sizeError}</p>}
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
      <button type="submit" disabled={hasError}>
        Dokument anlegen
      </button>
    </form>
  )
}
