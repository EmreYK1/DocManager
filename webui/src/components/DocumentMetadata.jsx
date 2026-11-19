import StatusBadge from './StatusBadge.jsx'
import { statusInfo } from '../constants/documentStatus.js'
import { formatDate } from '../utils/formatDate.js'
import { formatSize } from '../utils/formatSize.js'

function fileTypeLabel(contentType) {
  return contentType === 'application/pdf' ? 'PDF' : contentType || '–'
}

export default function DocumentMetadata({ document, folderName }) {
  const { description } = statusInfo(document.status)
  return (
    <>
      <dl className="meta">
        <div>
          <dt>Dateiname</dt>
          <dd>{document.filename}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <StatusBadge status={document.status} />
          </dd>
        </div>
        <div>
          <dt>Ordner</dt>
          <dd>{folderName}</dd>
        </div>
        <div>
          <dt>Dateityp</dt>
          <dd>{fileTypeLabel(document.contentType)}</dd>
        </div>
        <div>
          <dt>Größe</dt>
          <dd>{formatSize(document.sizeBytes)}</dd>
        </div>
        <div>
          <dt>Hochgeladen am</dt>
          <dd>{formatDate(document.uploadedAt)}</dd>
        </div>
      </dl>
      {description && <p className="status-note">{description}</p>}
    </>
  )
}
