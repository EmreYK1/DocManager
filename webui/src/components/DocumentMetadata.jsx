import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/formatDate.js'

export default function DocumentMetadata({ document, folderName }) {
  return (
    <dl className="meta">
      <div>
        <dt>Dateiname</dt>
        <dd>{document.filename}</dd>
      </div>
      <div>
        <dt>Content-Type</dt>
        <dd>{document.contentType}</dd>
      </div>
      <div>
        <dt>Größe</dt>
        <dd>{document.sizeBytes} Bytes</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd>
          <StatusBadge status={document.status} />
        </dd>
      </div>
      <div>
        <dt>Upload-Datum</dt>
        <dd>{formatDate(document.uploadedAt)}</dd>
      </div>
      <div>
        <dt>Ordner</dt>
        <dd>{folderName}</dd>
      </div>
    </dl>
  )
}
