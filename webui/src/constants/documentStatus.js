// Status-Werte des Backends
export const DocumentStatus = Object.freeze({
  UPLOADED: 'UPLOADED',
  OCR_PENDING: 'OCR_PENDING',
  OCR_DONE: 'OCR_DONE',
  SUMMARY_DONE: 'SUMMARY_DONE',
  FAILED: 'FAILED',
})

// Verständliche Texte zu den technischen Status-Werten des Backends.
export const STATUS_INFO = {
  UPLOADED: {
    label: 'Hochgeladen',
    tone: 'info',
    description: 'Das Dokument ist gespeichert. Die Texterkennung hat noch nicht begonnen.',
  },
  OCR_PENDING: {
    label: 'Text wird erkannt…',
    tone: 'warn',
    description:
      'Der Text wird gerade aus dem PDF gelesen. Das dauert meist nur wenige Sekunden, die Ansicht aktualisiert sich von selbst.',
  },
  OCR_DONE: {
    label: 'Durchsuchbar',
    tone: 'ok',
    description: 'Der Text wurde erkannt. Du findest das Dokument jetzt über die Suche.',
  },
  SUMMARY_DONE: {
    label: 'Zusammenfassung fertig',
    tone: 'ok',
    description: 'Der Text wurde erkannt und eine Zusammenfassung erstellt.',
  },
  FAILED: {
    label: 'Texterkennung fehlgeschlagen',
    tone: 'danger',
    description:
      'Der Text konnte nicht gelesen werden, z. B. weil das PDF beschädigt ist. Das Dokument ist gespeichert, aber nicht über die Suche auffindbar.',
  },
}

export function statusInfo(status) {
  return STATUS_INFO[status] ?? { label: status ?? 'Unbekannt', tone: 'info', description: '' }
}
