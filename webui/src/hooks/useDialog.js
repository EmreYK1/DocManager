import { useCallback, useRef, useState } from 'react'

// Verwaltet einen offenen Dialog samt Fehler. `run` führt eine Aktion aus,
// schließt den Dialog bei Erfolg und liefert true/false zurück.
// Läuft bereits eine Aktion, wird ein weiterer Aufruf ignoriert (Doppelklick-Schutz).
// Solange eine Aktion läuft (`busy`), lässt sich der Dialog nicht schließen,
// damit ein späterer Fehler nicht in einem bereits geschlossenen Dialog verschwindet.
export function useDialog() {
  const [current, setCurrent] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const running = useRef(false)

  const forceClose = useCallback(() => {
    setCurrent(null)
    setError(null)
  }, [])

  const close = useCallback(() => {
    if (running.current) return
    forceClose()
  }, [forceClose])

  const open = useCallback((name) => {
    setError(null)
    setCurrent(name)
  }, [])

  const run = useCallback(
    async (action) => {
      if (running.current) return false
      running.current = true
      setBusy(true)
      setError(null)
      try {
        await action()
        forceClose()
        return true
      } catch (err) {
        setError(err)
        return false
      } finally {
        running.current = false
        setBusy(false)
      }
    },
    [forceClose],
  )

  return { current, error, busy, open, close, run }
}
