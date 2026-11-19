import Modal from './Modal.jsx'
import ErrorBanner from './ErrorBanner.jsx'

export default function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Löschen',
  error,
  busy = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal title={title} onClose={onCancel} busy={busy}>
      <p>{message}</p>
      <ErrorBanner error={error} />
      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={busy}>
          Abbrechen
        </button>
        <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Bitte warten…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
