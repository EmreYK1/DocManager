// Setzt ein Formular zurück, sobald das Absenden erfolgreich war:
// sofort bei synchronem Handler, sonst nach dem Promise (bei Fehler oder `false` bleibt die Eingabe erhalten).
export function resetAfterSubmit(result, reset) {
  if (result && typeof result.then === 'function') {
    result.then((ok) => {
      if (ok !== false) reset()
    }, () => {})
  } else {
    reset()
  }
}
