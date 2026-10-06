import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './httpClient.js'

describe('request', () => {
  let fetchMock

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('liefert den JSON-Body bei Erfolg', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify([{ id: 1 }]), { status: 200 }))

    await expect(request('/documents')).resolves.toEqual([{ id: 1 }])
    expect(fetchMock.mock.calls[0][0]).toBe('/api/documents')
  })

  it('liefert null bei Status 204', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(request('/documents/1', { method: 'DELETE' })).resolves.toBeNull()
  })

  it('wirft ApiError mit Status 500 bei einem Serverfehler', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ status: 500, message: 'Boom' }), { status: 500 }),
    )

    const promise = request('/documents')

    await expect(promise).rejects.toBeInstanceOf(ApiError)
    await expect(promise).rejects.toMatchObject({ status: 500, message: 'Boom' })
  })

  it('wirft ApiError mit Status 0 bei einem Netzwerkfehler', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(request('/documents')).rejects.toMatchObject({ status: 0 })
  })
})
