import { useState } from 'react'
import FolderSelect from './FolderSelect.jsx'
import TextField from './TextField.jsx'
import { useTextField } from '../hooks/useTextField.js'

export default function FolderForm({
  folders = [],
  onSubmit,
  initial = { name: '', parentId: '' },
  excludeIds,
  submitLabel = 'Ordner anlegen',
  busy = false,
}) {
  const name = useTextField('Ordnername', initial.name)
  const [parentId, setParentId] = useState(initial.parentId ?? '')

  function handleSubmit(event) {
    event.preventDefault()
    if (name.error) return
    onSubmit({ name: name.value.trim(), parentId: parentId || null })
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      <TextField label="Name" field={name} autoFocus />
      <FolderSelect
        label="Elternordner"
        value={parentId}
        onChange={setParentId}
        folders={folders}
        excludeIds={excludeIds}
        emptyLabel="– kein Elternordner –"
      />
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={Boolean(name.error) || busy}>
          {submitLabel}
        </button>
      </div>
    </form>
  )
}
