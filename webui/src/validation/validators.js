export function requireText(value, fieldName) {
  if (typeof value !== 'string' || value.trim() === '') {
    return `${fieldName} darf nicht leer sein.`
  }
  return null
}

// Manche Systeme (z. B. Windows ohne PDF-Zuordnung) melden für PDFs einen leeren
// oder abweichenden MIME-Typ. Dann entscheidet die Endung.
const LOOSE_PDF_TYPES = ['', 'application/x-pdf', 'application/octet-stream']

export function isPdfFile(file) {
  if (file.type === 'application/pdf') return true
  return LOOSE_PDF_TYPES.includes(file.type) && /\.pdf$/i.test(file.name)
}

export function requirePdf(file, fieldName) {
  if (!file) {
    return `${fieldName} muss ausgewählt werden.`
  }
  if (!isPdfFile(file)) {
    return `${fieldName} muss eine PDF-Datei sein.`
  }
  if (file.size === 0) {
    return `${fieldName} darf nicht leer sein.`
  }
  return null
}

export const MAX_UPLOAD_BYTES = 50 * 1024 * 1024

export function maxFileSize(file, fieldName, maxBytes = MAX_UPLOAD_BYTES) {
  if (file && file.size > maxBytes) {
    return `${fieldName} ist zu groß (höchstens ${Math.round(maxBytes / 1024 / 1024)} MB).`
  }
  return null
}
