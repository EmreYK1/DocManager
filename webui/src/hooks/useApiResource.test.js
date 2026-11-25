import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { useApiResource } from './useApiResource.js'

describe('useApiResource', () => {
  afterEach(cleanup)

  it('behält den Fehler beim Neuladen bis zum nächsten Erfolg (kein Flackern)', async () => {
    const error = { status: 0 }
    let release
    const loader = vi
      .fn()
      .mockRejectedValueOnce(error)
      .mockImplementationOnce(() => new Promise((resolve) => (release = resolve)))

    const { result } = renderHook(() => useApiResource(loader))
    await waitFor(() => expect(result.current.error).toBe(error))

    act(() => result.current.reload())
    await waitFor(() => expect(result.current.loading).toBe(true))
    expect(result.current.error).toBe(error)

    await act(async () => release(['doc']))
    expect(result.current.error).toBeNull()
    expect(result.current.data).toEqual(['doc'])
  })

  it('verwirft den Fehler, wenn sich der Loader ändert', async () => {
    const error = { status: 500 }
    const failing = vi.fn().mockRejectedValue(error)
    const pending = vi.fn(() => new Promise(() => {}))

    const { result, rerender } = renderHook(({ loader }) => useApiResource(loader), {
      initialProps: { loader: failing },
    })
    await waitFor(() => expect(result.current.error).toBe(error))

    rerender({ loader: pending })

    await waitFor(() => expect(result.current.error).toBeNull())
  })
})
