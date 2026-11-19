import { useEffect, useRef, useState } from 'react'

export const OCR_POLL_INTERVAL_MS = 3000
export const OCR_POLL_MAX_DURATION_MS = 5 * 60 * 1000
export const OCR_POLL_MAX_BACKOFF_MS = 30 * 1000

function useTabVisible() {
  const [visible, setVisible] = useState(() => document.visibilityState !== 'hidden')
  useEffect(() => {
    const update = () => setVisible(document.visibilityState !== 'hidden')
    document.addEventListener('visibilitychange', update)
    return () => document.removeEventListener('visibilitychange', update)
  }, [])
  return visible
}

// Lädt regelmäßig neu, solange `active` gilt (z. B. ein Dokument ist OCR_PENDING).
// - Der nächste Reload wird erst geplant, wenn der vorige fertig ist (`busy` false),
//   so bricht ein langsamer Request nie den eigenen Vorgänger ab.
// - Bei Fehlern wird der Abstand verdoppelt (bis maxBackoffMs).
// - Im Hintergrund-Tab wird pausiert, nach maxDurationMs aufgegeben.
// - `resetKey` beschreibt, WAS gerade wartet (z. B. die IDs der wartenden Dokumente).
//   Ändert er sich, startet das Zeitbudget neu: Ein hängendes altes Dokument darf das
//   Beobachten eines neu hochgeladenen nicht verhindern.
// Rückgabe: `gaveUp` ist true, wenn das Zeitbudget aufgebraucht ist.
export function useOcrPolling({
  active,
  busy,
  failed,
  reload,
  resetKey = '',
  intervalMs = OCR_POLL_INTERVAL_MS,
  maxDurationMs = OCR_POLL_MAX_DURATION_MS,
  maxBackoffMs = OCR_POLL_MAX_BACKOFF_MS,
}) {
  const visible = useTabVisible()
  const startedAt = useRef(null)
  const lastKey = useRef(resetKey)
  const failures = useRef(0)
  const [gaveUp, setGaveUp] = useState(false)

  useEffect(() => {
    if (!active) {
      startedAt.current = null
      failures.current = 0
      setGaveUp(false)
      return undefined
    }
    if (startedAt.current === null || lastKey.current !== resetKey) {
      startedAt.current = Date.now()
      lastKey.current = resetKey
      setGaveUp(false)
    }
    if (busy || !visible) return undefined
    if (Date.now() - startedAt.current >= maxDurationMs) {
      setGaveUp(true)
      return undefined
    }

    failures.current = failed ? failures.current + 1 : 0
    const delay = failed ? Math.min(intervalMs * 2 ** failures.current, maxBackoffMs) : intervalMs
    const timer = setTimeout(reload, delay)
    return () => clearTimeout(timer)
  }, [active, busy, failed, visible, reload, resetKey, intervalMs, maxDurationMs, maxBackoffMs])

  return { gaveUp }
}
