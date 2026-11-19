import { useEffect, useRef } from 'react'

// `busy`: eine Aktion läuft, der Dialog lässt sich solange nicht schließen.
export default function Modal({ title, onClose, busy = false, children }) {
  const onCloseRef = useRef(onClose)
  onCloseRef.current = busy ? () => {} : onClose

  useEffect(() => {
    function onKeyDown(event) {
      if (event.key === 'Escape') onCloseRef.current()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  return (
    <div className="modal-backdrop" onMouseDown={busy ? undefined : onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-busy={busy}
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <h2>{title}</h2>
          <button
            type="button"
            className="icon-button"
            aria-label="Schließen"
            disabled={busy}
            title={busy ? 'Bitte warten, die Aktion läuft noch.' : undefined}
            onClick={onClose}
          >
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
