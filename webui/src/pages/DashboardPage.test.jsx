import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from './DashboardPage.jsx'
import { getDocuments, searchDocuments, uploadDocument } from '../api/documentApi.js'
import { getFolders } from '../api/folderApi.js'

vi.mock('../api/documentApi.js', () => ({
  getDocuments: vi.fn(),
  searchDocuments: vi.fn(),
  uploadDocument: vi.fn(),
}))
vi.mock('../api/folderApi.js', () => ({
  getFolders: vi.fn(),
  createFolder: vi.fn(),
  updateFolder: vi.fn(),
  deleteFolder: vi.fn(),
}))

const hello = {
  id: '1',
  filename: 'HelloWorld.pdf',
  status: 'OCR_DONE',
  folderId: null,
  sizeBytes: 593,
  uploadedAt: '2026-11-19T20:00:00Z',
}
const invoice = { ...hello, id: '2', filename: 'Rechnung.pdf' }
const inFolder = { ...hello, id: '3', filename: 'Hello-Vertrag.pdf', folderId: 'f1' }

function renderPage(url = '/') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <DashboardPage />
    </MemoryRouter>,
  )
}

function search(text) {
  fireEvent.change(screen.getByLabelText('Dokumente durchsuchen'), { target: { value: text } })
  fireEvent.click(screen.getByRole('button', { name: 'Suchen' }))
}

describe('DashboardPage Volltextsuche', () => {
  beforeEach(() => {
    getDocuments.mockResolvedValue([hello, invoice, inFolder])
    getFolders.mockResolvedValue([{ id: 'f1', name: 'Verträge', parentId: null }])
    searchDocuments.mockResolvedValue([hello])
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('zeigt zuerst alle Dokumente und sucht nicht ohne Eingabe', async () => {
    renderPage()

    expect(await screen.findByText('Rechnung.pdf')).toBeTruthy()
    expect(searchDocuments).not.toHaveBeenCalled()
  })

  it('sucht erst beim Absenden, nicht bei jedem Tastendruck', async () => {
    renderPage()
    await screen.findByText('Rechnung.pdf')

    fireEvent.change(screen.getByLabelText('Dokumente durchsuchen'), { target: { value: 'Hel' } })
    fireEvent.change(screen.getByLabelText('Dokumente durchsuchen'), { target: { value: 'Hello' } })
    expect(searchDocuments).not.toHaveBeenCalled()

    fireEvent.submit(screen.getByRole('search'))
    await waitFor(() => expect(searchDocuments).toHaveBeenCalledTimes(1))
    expect(searchDocuments).toHaveBeenCalledWith('Hello')
  })

  it('Suche nach „Hello“ zeigt nur HelloWorld.pdf', async () => {
    renderPage()
    await screen.findByText('Rechnung.pdf')

    search('Hello')

    expect(await screen.findByText('HelloWorld.pdf')).toBeTruthy()
    expect(screen.queryByText('Rechnung.pdf')).toBeNull()
  })

  it('zeigt „Keine Treffer für xyzabc“ ohne Ergebnis', async () => {
    searchDocuments.mockResolvedValue([])
    renderPage()
    await screen.findByText('Rechnung.pdf')

    search('xyzabc')

    expect(await screen.findByText('Keine Treffer für xyzabc')).toBeTruthy()
  })

  it('„Suche zurücksetzen“ zeigt wieder alle Dokumente', async () => {
    renderPage()
    await screen.findByText('Rechnung.pdf')
    search('Hello')
    await screen.findByText('HelloWorld.pdf')

    fireEvent.click(screen.getByRole('button', { name: 'Suche zurücksetzen' }))

    expect(await screen.findByText('Rechnung.pdf')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Suche zurücksetzen' })).toBeNull()
    expect(screen.getByLabelText('Dokumente durchsuchen').value).toBe('')
  })

  it('wendet den Ordnerfilter auf die Treffer an', async () => {
    searchDocuments.mockResolvedValue([hello, inFolder])
    renderPage('/?folder=f1')
    await screen.findByText('Hello-Vertrag.pdf')

    search('Hello')

    await waitFor(() => expect(searchDocuments).toHaveBeenCalled())
    await screen.findByText('Hello-Vertrag.pdf')
    expect(screen.queryByText('HelloWorld.pdf')).toBeNull()
  })

  it('zeigt einen Fehler, wenn die Suche fehlschlägt', async () => {
    searchDocuments.mockRejectedValue(new Error('Suche derzeit nicht verfügbar'))
    renderPage()
    await screen.findByText('Rechnung.pdf')

    search('Hello')

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.queryByText(/Keine Treffer/)).toBeNull()
  })

  it('zeigt bei einer wiederholten Suche nach einem Fehler „Suche läuft…“', async () => {
    searchDocuments.mockRejectedValueOnce(new Error('Suche derzeit nicht verfügbar'))
    renderPage()
    await screen.findByText('Rechnung.pdf')
    search('Hello')
    await screen.findByRole('alert')

    let finish
    searchDocuments.mockImplementationOnce(() => new Promise((resolve) => (finish = resolve)))
    fireEvent.click(screen.getByRole('button', { name: 'Suchen' }))

    expect(await screen.findByText('Suche läuft…')).toBeTruthy()
    await act(async () => finish([hello]))
    expect(await screen.findByText('HelloWorld.pdf')).toBeTruthy()
  })
})

describe('DashboardPage PDF-Upload', () => {
  beforeEach(() => {
    getDocuments.mockResolvedValue([invoice])
    getFolders.mockResolvedValue([])
    uploadDocument.mockResolvedValue(hello)
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('lädt HelloWorld.pdf hoch und lädt die Tabelle neu', async () => {
    renderPage()
    await screen.findByText('Rechnung.pdf')

    fireEvent.click(screen.getByRole('button', { name: 'PDF hochladen' }))
    const file = new File(['%PDF-1.4'], 'HelloWorld.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF-Datei'), { target: { files: [file] } })
    getDocuments.mockResolvedValue([invoice, hello])
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))

    expect(await screen.findByText('HelloWorld.pdf')).toBeTruthy()
    expect(uploadDocument).toHaveBeenCalledWith(file, null)
    await waitFor(() => expect(screen.queryByLabelText('PDF-Datei')).toBeNull())
    expect(screen.getByRole('status').textContent).toMatch(/„HelloWorld.pdf“ wurde hochgeladen/)
  })

  it('zeigt einen Hinweis, solange Text erkannt wird', async () => {
    getDocuments.mockResolvedValue([{ ...hello, status: 'OCR_PENDING' }])
    renderPage()

    expect(await screen.findByText(/Bei 1 Dokument wird gerade der Text erkannt/)).toBeTruthy()
    expect(screen.getByText('Text wird erkannt…')).toBeTruthy()
  })

  it('erklärt den leeren Zustand ohne Dokumente', async () => {
    getDocuments.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Noch keine Dokumente.')).toBeTruthy()
    expect(screen.getByText(/Lade dein erstes PDF/)).toBeTruthy()
    expect(screen.getByText('0 Dokumente')).toBeTruthy()
  })
})

describe('DashboardPage Neuladen bei OCR_PENDING', () => {
  const pending = { ...hello, status: 'OCR_PENDING' }
  const done = { ...hello, status: 'OCR_DONE' }

  beforeEach(() => {
    vi.useFakeTimers()
    getFolders.mockResolvedValue([])
  })

  afterEach(() => {
    cleanup()
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  const advance = (ms) =>
    act(async () => {
      await vi.advanceTimersByTimeAsync(ms)
    })

  it('lädt alle 3 Sekunden neu, solange ein Dokument OCR_PENDING ist, und hört danach auf', async () => {
    getDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)
    expect(getDocuments).toHaveBeenCalledTimes(1)

    await advance(2900)
    expect(getDocuments).toHaveBeenCalledTimes(1)
    await advance(100)
    expect(getDocuments).toHaveBeenCalledTimes(2)
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(3)

    // OCR fertig: der nächste Abruf liefert OCR_DONE, danach kommen keine Requests mehr
    getDocuments.mockResolvedValue([done])
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(4)

    await advance(15000)
    expect(getDocuments).toHaveBeenCalledTimes(4)
  })

  it('lädt nicht neu, wenn kein Dokument OCR_PENDING ist', async () => {
    getDocuments.mockResolvedValue([done])
    renderPage()
    await advance(0)

    await advance(15000)

    expect(getDocuments).toHaveBeenCalledTimes(1)
  })

  it('bricht einen langsamen Request nicht ab, sondern wartet ihn ab', async () => {
    getDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)

    // Zweiter Abruf dauert 5 s, länger als das Intervall
    let finishSlow
    getDocuments.mockImplementationOnce(() => new Promise((resolve) => (finishSlow = resolve)))
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(2)

    await advance(5000)
    expect(getDocuments).toHaveBeenCalledTimes(2)

    getDocuments.mockResolvedValue([done])
    await act(async () => finishSlow([done]))
    expect(screen.getByText('HelloWorld.pdf')).toBeTruthy()
    await advance(15000)
    expect(getDocuments).toHaveBeenCalledTimes(2)
  })

  it('hört nach spätestens 5 Minuten auf, auch wenn die OCR nie fertig wird', async () => {
    getDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)

    // In 3-s-Schritten, damit React zwischen den Abrufen rendern kann
    for (let elapsed = 0; elapsed < 5 * 60 * 1000; elapsed += 3000) {
      await advance(3000)
    }
    const callsAfterLimit = getDocuments.mock.calls.length
    expect(callsAfterLimit).toBeGreaterThan(90)

    for (let i = 0; i < 20; i++) {
      await advance(3000)
    }
    expect(getDocuments).toHaveBeenCalledTimes(callsAfterLimit)
  })

  it('pausiert im Hintergrund-Tab und macht danach weiter', async () => {
    getDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)

    const visibility = vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden')
    await act(async () => document.dispatchEvent(new Event('visibilitychange')))
    await advance(15000)
    expect(getDocuments).toHaveBeenCalledTimes(1)

    visibility.mockReturnValue('visible')
    await act(async () => document.dispatchEvent(new Event('visibilitychange')))
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(2)
    visibility.mockRestore()
  })

  it('verlängert den Abstand bei Fehlern und zeigt den Fehler dauerhaft', async () => {
    getDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)

    getDocuments.mockRejectedValue(new Error('Netzwerkfehler'))
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('alert')).toBeTruthy()

    // Nach einem Fehler: 6 s statt 3 s
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('alert')).toBeTruthy()
    await advance(3000)
    expect(getDocuments).toHaveBeenCalledTimes(3)
    expect(screen.getByRole('alert')).toBeTruthy()
  })

  it('lädt bei aktiver Suche auch die Treffer neu', async () => {
    getDocuments.mockResolvedValue([pending])
    searchDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)

    fireEvent.change(screen.getByLabelText('Dokumente durchsuchen'), { target: { value: 'Hello' } })
    fireEvent.submit(screen.getByRole('search'))
    await advance(0)
    expect(searchDocuments).toHaveBeenCalledTimes(1)

    searchDocuments.mockResolvedValue([done])
    getDocuments.mockResolvedValue([done])
    await advance(3000)
    expect(searchDocuments).toHaveBeenCalledTimes(2)
    expect(screen.getByText('Durchsuchbar')).toBeTruthy()
  })

  it('beobachtet auch ein neues Dokument, wenn ein altes dauerhaft hängt', async () => {
    const stuck = { ...hello, id: 'stuck', filename: 'Haengt.pdf', status: 'OCR_PENDING' }
    const fresh = { ...hello, id: 'fresh', filename: 'Neu.pdf', status: 'OCR_PENDING' }
    getDocuments.mockResolvedValue([stuck])
    renderPage()
    await advance(0)

    // Das alte Dokument hängt länger als das 5-Minuten-Budget
    for (let elapsed = 0; elapsed < 5 * 60 * 1000 + 6000; elapsed += 3000) {
      await advance(3000)
    }
    expect(screen.getByText(/Das dauert ungewöhnlich lange/)).toBeTruthy()
    const callsWhenGivenUp = getDocuments.mock.calls.length
    await advance(15000)
    expect(getDocuments).toHaveBeenCalledTimes(callsWhenGivenUp)

    // Ein neues Dokument wird hochgeladen: Das Beobachten beginnt von vorn
    getDocuments.mockResolvedValue([stuck, fresh])
    uploadDocument.mockResolvedValue(fresh)
    fireEvent.click(screen.getByRole('button', { name: 'PDF hochladen' }))
    const file = new File(['%PDF-1.4'], 'Neu.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('PDF-Datei'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))
    await advance(0)

    expect(screen.queryByText(/Das dauert ungewöhnlich lange/)).toBeNull()
    expect(screen.getByText(/Die Liste aktualisiert sich automatisch/)).toBeTruthy()
    getDocuments.mockResolvedValue([stuck, { ...fresh, status: 'OCR_DONE' }])
    const callsBefore = getDocuments.mock.calls.length
    await advance(3000)
    expect(getDocuments.mock.calls.length).toBeGreaterThan(callsBefore)
    expect(screen.getByText('Durchsuchbar')).toBeTruthy()
  })

  it('zeigt Treffer weiter an, während sie im Hintergrund neu geladen werden', async () => {
    getDocuments.mockResolvedValue([pending])
    searchDocuments.mockResolvedValue([pending])
    renderPage()
    await advance(0)
    fireEvent.change(screen.getByLabelText('Dokumente durchsuchen'), { target: { value: 'Hello' } })
    fireEvent.submit(screen.getByRole('search'))
    await advance(0)

    let finishReload
    searchDocuments.mockImplementationOnce(() => new Promise((resolve) => (finishReload = resolve)))
    await advance(3000)

    expect(screen.queryByText('Suche läuft…')).toBeNull()
    expect(screen.getByText('HelloWorld.pdf')).toBeTruthy()
    await act(async () => finishReload([pending]))
  })

  it('räumt den Timer beim Verlassen der Seite auf', async () => {
    getDocuments.mockResolvedValue([pending])
    const { unmount } = renderPage()
    await advance(0)

    unmount()
    await advance(15000)

    expect(getDocuments).toHaveBeenCalledTimes(1)
  })
})
