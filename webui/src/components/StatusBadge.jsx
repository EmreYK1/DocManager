import { DocumentStatus, statusInfo } from '../constants/documentStatus.js'

export default function StatusBadge({ status }) {
  const { label, tone, description } = statusInfo(status)
  return (
    <span className={`badge badge-${tone}`} title={description}>
      {status === DocumentStatus.OCR_PENDING && <span className="spinner" aria-hidden="true" />}
      {label}
    </span>
  )
}
