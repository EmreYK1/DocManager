import { formatDate } from '../utils/formatDate.js'

export default function CommentList({ comments }) {
  if (comments.length === 0) {
    return <p>Keine Kommentare</p>
  }

  return (
    <ul>
      {comments.map((comment) => (
        <li key={comment.id}>
          <strong>{comment.author}</strong> ({formatDate(comment.createdAt)}): {comment.content}
        </li>
      ))}
    </ul>
  )
}
