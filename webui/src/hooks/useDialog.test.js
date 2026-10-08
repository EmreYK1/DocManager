import { describe, expect, it } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useDialog } from './useDialog.js'

describe('useDialog', () => {
  it('schließt den Dialog nach erfolgreicher Aktion', async () => {
    const { result } = renderHook(() => useDialog())
    act(() => result.current.open('edit'))

    let ok
    await act(async () => {
      ok = await result.current.run(async () => {})
    })

    expect(ok).toBe(true)
    expect(result.current.current).toBeNull()
  })

  it('lässt den Dialog offen und merkt sich den Fehler, wenn die Aktion scheitert', async () => {
    const { result } = renderHook(() => useDialog())
    act(() => result.current.open('edit'))
    const error = new Error('Boom')

    let ok
    await act(async () => {
      ok = await result.current.run(async () => {
        throw error
      })
    })

    expect(ok).toBe(false)
    expect(result.current.current).toBe('edit')
    expect(result.current.error).toBe(error)
  })

  it('ignoriert einen zweiten Aufruf, solange die Aktion läuft', async () => {
    const { result } = renderHook(() => useDialog())
    let calls = 0
    let release
    const slow = () => {
      calls++
      return new Promise((resolve) => {
        release = resolve
      })
    }

    let first
    let second
    await act(async () => {
      first = result.current.run(slow)
      second = await result.current.run(slow)
      release()
      await first
    })

    expect(calls).toBe(1)
    expect(second).toBe(false)
  })
})
