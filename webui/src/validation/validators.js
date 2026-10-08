export function requireText(value, fieldName) {
  if (typeof value !== 'string' || value.trim() === '') {
    return `${fieldName} darf nicht leer sein.`
  }
  return null
}

export function nonNegativeInteger(value, fieldName) {
  const text = typeof value === 'string' ? value.trim() : ''
  if (text === '' || !Number.isInteger(Number(text)) || Number(text) < 0) {
    return `${fieldName} muss eine ganze Zahl ab 0 sein.`
  }
  return null
}
