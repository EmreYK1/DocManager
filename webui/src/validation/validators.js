export function requireText(value, fieldName) {
  if (typeof value !== 'string' || value.trim() === '') {
    return `${fieldName} darf nicht leer sein.`
  }
  return null
}
