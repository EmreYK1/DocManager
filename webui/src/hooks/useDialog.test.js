import { describe, expect, it } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useDialog } from './useDialog.js'

describe('useDialog', () => {
  it('lässt sich nicht schließen, solange eine Aktion läuft, und zeigt danach den Fehler', async () => {
    const { result } = renderHook(() => useDialog())
    act(() => result.current.open('upload'))
    let fail
    let running
    act(() => {
      running = result.current.run(
        () =>
          new Promise((_, reject) => {
            fail = reject
          }),
      )
    })

    expect(result.current.busy).toBe(true)
    act(() => result.current.close())
    expect(result.current.current).toBe('upload')

    const error = new Error('413')
    await act(async () => {
      fail(error)
      await running
    })

    expect(result.current.busy).toBe(false)
    expect(result.current.current).toBe('upload')
    expect(result.current.error).toBe(error)
    act(() => result.current.close())
    expect(result.current.current).toBeNull()
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
