import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, renderHook, waitFor } from '@testing-library/react'
import { useApiResource } from './useApiResource.js'

describe('useApiResource', () => {
  afterEach(cleanup)

  it('lädt Daten und beendet den Ladezustand', async () => {
    const loader = vi.fn().mockResolvedValue(['doc'])

    const { result } = renderHook(() => useApiResource(loader))

    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.data).toEqual(['doc'])
    expect(result.current.error).toBeNull()
  })

  it('speichert den Fehler, wenn das Laden scheitert', async () => {
    const error = { status: 500 }
    const loader = vi.fn().mockRejectedValue(error)

    const { result } = renderHook(() => useApiResource(loader))

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBe(error)
    expect(result.current.data).toBeNull()
  })

  it('lädt neu, wenn reload aufgerufen wird', async () => {
    const loader = vi.fn().mockResolvedValue([])

    const { result } = renderHook(() => useApiResource(loader))

    await waitFor(() => expect(result.current.loading).toBe(false))
    result.current.reload()
    await waitFor(() => expect(loader).toHaveBeenCalledTimes(2))
  })
})
