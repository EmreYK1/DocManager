import { useCallback, useEffect, useRef, useState } from 'react'

// `loader` muss stabil sein (z. B. eine Funktion aus einem API-Modul),
// sonst wird bei jedem Render neu geladen.
export function useApiResource(loader) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)
  const lastLoader = useRef(loader)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    // Bei reload() bleibt ein alter Fehler bis zum nächsten Erfolg stehen (kein Flackern beim Neuladen).
    // Bei einem neuen Loader gehört der alte Fehler nicht mehr dazu.
    if (lastLoader.current !== loader) {
      lastLoader.current = loader
      setError(null)
    }

    loader()
      .then((result) => {
        if (!cancelled) {
          setData(result)
          setError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [loader, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])

  return { data, loading, error, reload }
}
