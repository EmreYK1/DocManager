import { Link } from 'react-router-dom'
import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/formatDate.js'
import { formatSize } from '../utils/formatSize.js'

export default function DocumentTable({ documents, folderNames = new Map() }) {
  if (documents.length === 0) {
    return <p className="empty">Keine Dokumente vorhanden.</p>
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Dateiname</th>
            <th>Status</th>
            <th>Ordner</th>
            <th>Größe</th>
            <th>Upload-Datum</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((document) => (
            <tr key={document.id}>
              <td>
                <Link to={`/documents/${document.id}`} className="doc-link">
                  {document.filename}
                </Link>
              </td>
              <td>
                <StatusBadge status={document.status} />
              </td>
              <td>{folderNames.get(document.folderId) ?? '–'}</td>
              <td>{formatSize(document.sizeBytes)}</td>
              <td>{formatDate(document.uploadedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
