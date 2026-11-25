import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './httpClient.ts'

describe('request', () => {
  let fetchMock

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('setzt bei JSON-Bodies den Content-Type application/json', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))

    await request('/documents', { method: 'POST', body: JSON.stringify({ a: 1 }) })

    expect(fetchMock.mock.calls[0][1].headers).toEqual({ 'Content-Type': 'application/json' })
  })

  it('setzt bei FormData keinen Content-Type, damit der Browser die Trennmarke ergänzt', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 200 }))
    const body = new FormData()
    body.append('file', new File(['x'], 'a.pdf', { type: 'application/pdf' }))

    await request('/documents/upload', { method: 'POST', body })

    const options = fetchMock.mock.calls[0][1]
    expect(options.body).toBe(body)
    expect(options.headers['Content-Type']).toBeUndefined()
  })

  it('liefert null bei Status 204', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(request('/documents/1', { method: 'DELETE' })).resolves.toBeNull()
  })

  it('übernimmt fields aus dem Fehler-Body bei Status 400', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 400,
          message: 'Validierungsfehler',
          fields: { content: 'Kommentarinhalt darf maximal 2000 Zeichen lang sein' },
        }),
        { status: 400 },
      ),
    )

    const promise = request('/documents/1/comments/2', {
      method: 'PUT',
      body: JSON.stringify({ content: 'x'.repeat(2001) }),
    })

    await expect(promise).rejects.toBeInstanceOf(ApiError)
    await expect(promise).rejects.toMatchObject({
      status: 400,
      message: 'Validierungsfehler',
      fields: { content: 'Kommentarinhalt darf maximal 2000 Zeichen lang sein' },
    })
  })

  it('wirft ApiError mit Status 0 bei einem Netzwerkfehler', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(request('/documents')).rejects.toMatchObject({ status: 0 })
  })
})
