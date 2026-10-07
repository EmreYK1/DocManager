import { useState } from 'react'

export default function FolderForm({ folders = [], onSubmit }) {
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({ name, parentId: parentId || null })
    setName('')
    setParentId('')
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        Name
        <input value={name} onChange={(event) => setName(event.target.value)} />
      </label>
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
      <button type="submit">Ordner anlegen</button>
    </form>
  )
}
