import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { searchDocuments } from './documentApi.js'

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
