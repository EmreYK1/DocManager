import { useCallback, useRef, useState } from 'react'

// Verwaltet einen offenen Dialog samt Fehler. `run` führt eine Aktion aus,
// schließt den Dialog bei Erfolg und liefert true/false zurück.
// Läuft bereits eine Aktion, wird ein weiterer Aufruf ignoriert (Doppelklick-Schutz).
export function useDialog() {
  const [current, setCurrent] = useState(null)
  const [error, setError] = useState(null)
  const running = useRef(false)

  const close = useCallback(() => {
    setCurrent(null)
    setError(null)
  }, [])

  const open = useCallback((name) => {
    setError(null)
    setCurrent(name)
  }, [])

  const run = useCallback(
    async (action) => {
      if (running.current) return false
      running.current = true
      setError(null)
      try {
        await action()
        close()
        return true
      } catch (err) {
        setError(err)
        return false
      } finally {
        running.current = false
      }
    },
    [close],
  )

  return { current, error, open, close, run }
}
