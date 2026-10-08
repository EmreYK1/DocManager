import { useState } from 'react'
import { requireText } from '../validation/validators.js'

export default function FolderForm({ folders = [], onSubmit }) {
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [touched, setTouched] = useState(false)
  const nameError = requireText(name, 'Ordnername')

  function handleSubmit(event) {
    event.preventDefault()
    if (nameError) return
    onSubmit({ name: name.trim(), parentId: parentId || null })
    setName('')
    setParentId('')
    setTouched(false)
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        Name
        <input
          value={name}
          onChange={(event) => {
            setName(event.target.value)
            setTouched(true)
          }}
        />
      </label>
      {touched && nameError && <p role="alert">{nameError}</p>}
      <label>
        Elternordner
        <select value={parentId} onChange={(event) => setParentId(event.target.value)}>
          <option value="">– kein Elternordner –</option>
          {folders.map((folder) => (
            <option key={folder.id} value={folder.id}>
              {folder.name}
            </option>
          ))}
        </select>
      </label>
      <button type="submit" disabled={Boolean(nameError)}>Ordner anlegen</button>
    </form>
  )
}
