import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { searchDocuments, uploadDocument } from './documentApi.js'

describe('uploadDocument', () => {
  let fetchMock
  const pdf = new File(['%PDF-1.4'], 'HelloWorld.pdf', { type: 'application/pdf' })

  beforeEach(() => {
    fetchMock = vi.fn().mockResolvedValue(new Response('{"id":"1"}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('schickt die Datei als multipart-FormData an /api/documents/upload', async () => {
    await uploadDocument(pdf, null)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/documents/upload')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    expect(options.body.get('file').name).toBe('HelloWorld.pdf')
    expect(options.body.has('folderId')).toBe(false)
    expect(options.headers['Content-Type']).toBeUndefined()
  })

  it('schickt eine .pdf ohne MIME-Typ als application/pdf', async () => {
    const untyped = new File(['%PDF-1.4'], 'Scan.pdf', { type: '' })

    await uploadDocument(untyped, null)

    const sent = fetchMock.mock.calls[0][1].body.get('file')
    expect(sent.type).toBe('application/pdf')
    expect(sent.name).toBe('Scan.pdf')
  })

  it('hängt die folderId an, wenn ein Ordner gewählt ist', async () => {
    await uploadDocument(pdf, 'f1')

    expect(fetchMock.mock.calls[0][1].body.get('folderId')).toBe('f1')
  })
})

describe('searchDocuments', () => {
  let fetchMock

  beforeEach(() => {
    fetchMock = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('ruft /api/documents/search mit dem Suchbegriff auf', async () => {
    await searchDocuments('Hello')

    expect(fetchMock.mock.calls[0][0]).toBe('/api/documents/search?q=Hello')
  })

  it('kodiert Leerzeichen und Umlaute', async () => {
    await searchDocuments('große Rechnung & mehr')

    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/documents/search?q=gro%C3%9Fe%20Rechnung%20%26%20mehr',
    )
  })
})
