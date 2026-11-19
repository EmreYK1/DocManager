import { Link } from 'react-router-dom'
import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/formatDate.js'
import { formatSize } from '../utils/formatSize.js'

export default function DocumentTable({
  documents,
  folderNames = new Map(),
  emptyMessage = 'Keine Dokumente vorhanden.',
  emptyHint = null,
  onDelete,
}) {
  if (documents.length === 0) {
    return (
      <div className="empty">
        <p className="empty-title">{emptyMessage}</p>
        {emptyHint && <p className="empty-hint">{emptyHint}</p>}
      </div>
    )
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
            <th>Hochgeladen am</th>
            {onDelete && (
              <th>
                <span className="visually-hidden">Aktionen</span>
              </th>
            )}
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
              {onDelete && (
                <td className="cell-actions">
                  <button
                    type="button"
                    className="btn btn-small btn-danger-ghost"
                    aria-label={`${document.filename} löschen`}
                    onClick={() => onDelete(document)}
                  >
                    Löschen
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
