import { DocumentStatus } from '../constants/documentStatus.js'

// Zeigt den per Texterkennung (OCR) gelesenen Inhalt eines Dokuments.
export default function RecognizedText({ status, text }) {
  if (text && text.trim()) {
    return <pre className="ocr-text">{text}</pre>
  }
  const message =
    status === DocumentStatus.OCR_PENDING
      ? 'Der Text wird gerade erkannt. Er erscheint hier, sobald die Erkennung fertig ist.'
      : status === DocumentStatus.UPLOADED
        ? 'Für dieses Dokument wurde noch kein Text erkannt.'
        : status === DocumentStatus.FAILED
          ? 'Der Text konnte nicht erkannt werden.'
          : 'In diesem Dokument wurde kein Text gefunden, z. B. weil es nur Bilder enthält.'
  return <p className="muted">{message}</p>
}
