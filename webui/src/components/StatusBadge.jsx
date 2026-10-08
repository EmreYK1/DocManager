const TONES = {
  UPLOADED: 'info',
  OCR_PENDING: 'warn',
  OCR_DONE: 'ok',
  SUMMARY_DONE: 'ok',
  FAILED: 'danger',
}

export default function StatusBadge({ status }) {
  return <span className={`badge badge-${TONES[status] ?? 'info'}`}>{status}</span>
}
