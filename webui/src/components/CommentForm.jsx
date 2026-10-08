import TextField from './TextField.jsx'
import { useTextField } from '../hooks/useTextField.js'
import { resetAfterSubmit } from '../utils/afterSubmit.js'

export default function CommentForm({ onSubmit }) {
  const author = useTextField('Autor')
  const content = useTextField('Kommentar')
  const hasError = Boolean(author.error || content.error)

  function handleSubmit(event) {
    event.preventDefault()
    if (hasError) return
    const result = onSubmit({ author: author.value.trim(), content: content.value.trim() })
    resetAfterSubmit(result, () => {
      author.reset()
      content.reset()
    })
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      <TextField label="Autor" field={author} />
      <TextField label="Kommentar" field={content} multiline />
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={hasError}>
          Kommentar senden
        </button>
      </div>
    </form>
  )
}
