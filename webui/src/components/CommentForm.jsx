import { useState } from 'react';

export default function CommentForm({ onSubmit }) {
    const [author, setAuthor] = useState('')
    const [content, setContent] = useState('')

    function handleSubmit(event) {
        event.preventDefault()
        onSubmit({ author, content })
            .then(() => {
                setAuthor('')
                setContent('')
            })
            .catch(() => {})
    }
    return (
        <form onSubmit={handleSubmit}>
            <label>
                Author
                <input value={author} onChange={(event) => setAuthor(event.target.value)} />
            </label>
                <label>
                Content
                <input value={content} onChange={(event) => setContent(event.target.value)} />
            </label>
            <button type="submit">Kommentar senden</button>
        </form> 
    )

}