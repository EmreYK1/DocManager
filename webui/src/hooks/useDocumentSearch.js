import { useCallback, useState } from 'react'
import { searchDocuments } from '../api/documentApi.ts'
import { useApiResource } from './useApiResource.js'

// `input` ist der Text im Feld, `query` der abgeschickte Suchbegriff.
export function useDocumentSearch() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')

  // Das Ergebnis trägt den Suchbegriff mit, damit Treffer einer älteren Suche nie angezeigt werden.
  const load = useCallback(
    () => (query ? searchDocuments(query).then((hits) => ({ query, hits })) : Promise.resolve(null)),
    [query],
  )
  const { data, loading, error, reload } = useApiResource(load)

  const active = query !== ''
  const ready = active && data?.query === query

  function submit() {
    const next = input.trim()
    if (next === query && next !== '') reload()
    setQuery(next)
  }

  function reset() {
    setInput('')
    setQuery('')
  }

  return {
    input,
    setInput,
    query,
    active,
    ready,
    hits: ready ? data.hits : [],
    loading: active && loading,
    error: active ? error : null,
    reload,
    submit,
    reset,
  }
}
