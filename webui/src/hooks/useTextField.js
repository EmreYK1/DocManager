import { useState } from 'react'
import { requireText } from '../validation/validators.js'

// Zustand eines Textfelds: Wert, Prüfergebnis und ob der Fehler schon angezeigt werden soll
// (erst nach der ersten Eingabe, damit ein frisches Formular nicht sofort rot ist).
export function useTextField(fieldName, initialValue = '', validate = requireText) {
  const [value, setValue] = useState(initialValue)
  const [touched, setTouched] = useState(false)
  const error = validate(value, fieldName)

  return {
    value,
    error,
    visibleError: touched ? error : null,
    onChange: (next) => {
      setValue(next)
      setTouched(true)
    },
    reset: () => {
      setValue(initialValue)
      setTouched(false)
    },
  }
}
