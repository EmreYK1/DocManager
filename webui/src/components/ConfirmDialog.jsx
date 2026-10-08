import Modal from './Modal.jsx'
import ErrorBanner from './ErrorBanner.jsx'

export default function ConfirmDialog({ title, message, confirmLabel = 'Löschen', error, onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p>{message}</p>
      <ErrorBanner error={error} />
      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel}>
          Abbrechen
        </button>
        <button type="button" className="btn btn-danger" onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
