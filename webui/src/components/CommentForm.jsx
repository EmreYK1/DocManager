import { useState } from 'react';
import { requireText } from '../validation/validators.js';

export default function CommentForm({ onSubmit }) {
    const [author, setAuthor] = useState('')
    const [content, setContent] = useState('')
    const [authorTouched, setAuthorTouched] = useState(false)
    const [contentTouched, setContentTouched] = useState(false)
    const authorError = requireText(author, 'Autor')
    const contentError = requireText(content, 'Inhalt')

    function handleSubmit(event) {
        event.preventDefault()
        if (authorError || contentError) return
        onSubmit({ author: author.trim(), content: content.trim() })
            .then(() => {
                setAuthor('')
                setContent('')
                setAuthorTouched(false)
                setContentTouched(false)
            })
            .catch(() => {})
    }
    return (
        <form onSubmit={handleSubmit}>
            <label>
                Author
                <input
                    value={author}
                    onChange={(event) => {
                        setAuthor(event.target.value)
                        setAuthorTouched(true)
                    }}
                />
            </label>
            {authorTouched && authorError && <p role="alert">{authorError}</p>}
                <label>
                Content
                <input
                    value={content}
                    onChange={(event) => {
                        setContent(event.target.value)
                        setContentTouched(true)
                    }}
                />
            </label>
            {contentTouched && contentError && <p role="alert">{contentError}</p>}
            <button type="submit" disabled={Boolean(authorError || contentError)}>Kommentar senden</button>
        </form> 
    )

}