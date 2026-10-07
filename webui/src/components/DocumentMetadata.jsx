import { formatDate } from '../utils/formatDate.js'

export default function DocumentMetadata({ document, folderName }) {
  return (
    <dl>
      <dt>Dateiname</dt>
      <dd>{document.filename}</dd>

      <dt>Content-Type</dt>
      <dd>{document.contentType}</dd>

      <dt>Größe</dt>
      <dd>{document.sizeBytes} Bytes</dd>

      <dt>Status</dt>
      <dd>{document.status}</dd>

      <dt>Upload-Datum</dt>
      <dd>{formatDate(document.uploadedAt)}</dd>

      <dt>Ordner</dt>
      <dd>{folderName}</dd>
    </dl>
  )
}
