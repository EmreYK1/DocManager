import { useState } from 'react'
import TextField from './TextField.jsx'
import { useTextField } from '../hooks/useTextField.js'
import { formatDate } from '../utils/formatDate.js'

function CommentItem({ comment, onUpdate, onDelete }) {
  const [editing, setEditing] = useState(false)
  const content = useTextField('Kommentar', comment.content)

  async function handleSave(event) {
    event.preventDefault()
    if (content.error) return
    const saved = await onUpdate(comment.id, content.value.trim())
    if (saved !== false) setEditing(false)
  }

  function handleCancel() {
    content.reset()
    setEditing(false)
  }

  return (
    <li className="comment">
      <div className="comment-head">
        <strong>{comment.author}</strong>
        <span className="muted small">({formatDate(comment.createdAt)})</span>
        {!editing && (onUpdate || onDelete) && (
          <span className="comment-actions">
            {onUpdate && (
              <button type="button" className="btn btn-small" onClick={() => setEditing(true)}>
                Bearbeiten
              </button>
            )}
            {onDelete && (
              <button type="button" className="btn btn-small btn-danger-ghost" onClick={() => onDelete(comment)}>
                Löschen
              </button>
            )}
          </span>
        )}
      </div>
      {editing ? (
        <form onSubmit={handleSave} className="form">
          <TextField label="Kommentar bearbeiten" field={content} multiline />
          <div className="form-actions">
            <button type="button" className="btn btn-small" onClick={handleCancel}>
              Abbrechen
            </button>
            <button type="submit" className="btn btn-small btn-primary" disabled={Boolean(content.error)}>
              Speichern
            </button>
          </div>
        </form>
      ) : (
        <p className="comment-body">{comment.content}</p>
      )}
    </li>
  )
}

export default function CommentList({ comments, onUpdate, onDelete }) {
  if (comments.length === 0) {
    return <p className="muted">Keine Kommentare</p>
  }

  return (
    <ul className="comments">
      {comments.map((comment) => (
        <CommentItem key={comment.id} comment={comment} onUpdate={onUpdate} onDelete={onDelete} />
      ))}
    </ul>
  )
}
