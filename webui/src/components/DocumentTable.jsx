import { Link } from 'react-router-dom'
import { formatDate } from '../utils/formatDate.js'

export default function DocumentTable({ documents, folderNames = new Map() }) {
  if (documents.length === 0) {
    return <p>Keine Dokumente vorhanden.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Dateiname</th>
          <th>Status</th>
          <th>Ordner</th>
          <th>Upload-Datum</th>
        </tr>
      </thead>
      <tbody>
        {documents.map((document) => (
          <tr key={document.id}>
            <td>
              <Link to={`/documents/${document.id}`}>{document.filename}</Link>
            </td>
            <td>{document.status}</td>
            <td>{folderNames.get(document.folderId) ?? '–'}</td>
            <td>{formatDate(document.uploadedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
