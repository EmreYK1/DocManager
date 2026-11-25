import { useEffect, useRef } from 'react'
import { DocumentStatus } from '../constants/documentStatus.js'
import { OCR_POLL_INTERVAL_MS, useOcrPolling } from './useOcrPolling.js'

const SEARCH_INDEX_DELAY_MS = OCR_POLL_INTERVAL_MS

// Lädt neu, solange bei einem der Dokumente die Texterkennung läuft, damit der Status ohne F5 wechselt.
export function usePendingOcrRefresh({ documents, search, busy, failed, reload }) {
  const pendingIds = documents.filter((doc) => doc.status === DocumentStatus.OCR_PENDING).map((doc) => doc.id)
  const hasPending = pendingIds.length > 0
  const polling = useOcrPolling({
    active: hasPending,
    busy,
    failed,
    reload,
    // Wartet ein anderes Dokument als vorher, beginnt das Zeitbudget neu.
    resetKey: [...new Set(pendingIds)].sort().join(','),
  })

  // Der Suchindex wird erst kurz nach der Texterkennung befüllt: Treffer danach einmal nachladen.
  const wasPending = useRef(false)
  const { active: searching, reload: reloadSearch } = search
  useEffect(() => {
    const finished = wasPending.current && !hasPending
    wasPending.current = hasPending
    if (!finished || !searching) return undefined
    const timer = setTimeout(reloadSearch, SEARCH_INDEX_DELAY_MS)
    return () => clearTimeout(timer)
  }, [hasPending, searching, reloadSearch])

  return polling
}
