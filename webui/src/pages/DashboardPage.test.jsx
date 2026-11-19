import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DashboardPage from './DashboardPage.jsx'
import { getDocuments, searchDocuments } from '../api/documentApi.js'
import { getFolders } from '../api/folderApi.js'

vi.mock('../api/documentApi.js', () => ({
  getDocuments: vi.fn(),
  searchDocuments: vi.fn(),
  createDocument: vi.fn(),
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
})
