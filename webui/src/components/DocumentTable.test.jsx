import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import DocumentTable from './DocumentTable.jsx'

const documents = [
  { id: '1', filename: 'bericht.pdf', status: 'UPLOADED', uploadedAt: '2026-10-06T10:00:00Z', folderId: 'f1' },
  { id: '2', filename: 'rechnung.pdf', status: 'OCR_DONE', uploadedAt: '2026-10-05T08:30:00Z', folderId: null },
]

function renderTable(props) {
  return render(
    <MemoryRouter>
      <DocumentTable {...props} />
    </MemoryRouter>,
  )
}

describe('DocumentTable', () => {
  afterEach(cleanup)

  it('zeigt Dateiname und Status pro Zeile', () => {
    renderTable({ documents })

    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3)
    expect(rows[1].textContent).toContain('bericht.pdf')
    expect(rows[1].textContent).toContain('UPLOADED')
    expect(rows[2].textContent).toContain('rechnung.pdf')
    expect(rows[2].textContent).toContain('OCR_DONE')
  })

  it('verlinkt den Dateinamen auf die Detailseite des Dokuments', () => {
    renderTable({ documents })

    const link = screen.getByRole('link', { name: 'bericht.pdf' })
    expect(link.getAttribute('href')).toBe('/documents/1')
  })

  it('zeigt den Ordnernamen statt der UUID, wenn das Dokument einen Ordner hat', () => {
    const folderNames = new Map([['f1', 'Rechnungen']])

    renderTable({ documents, folderNames })

    const rows = screen.getAllByRole('row')
    expect(rows[1].textContent).toContain('Rechnungen')
  })

  it('zeigt „–“, wenn das Dokument keinem Ordner zugeordnet ist', () => {
    const folderNames = new Map([['f1', 'Rechnungen']])

    renderTable({ documents, folderNames })

    const rows = screen.getAllByRole('row')
    expect(rows[2].textContent).toContain('–')
  })

  it('zeigt einen Hinweis, wenn keine Dokumente vorhanden sind', () => {
    renderTable({ documents: [] })

    expect(screen.getByText('Keine Dokumente vorhanden.')).toBeTruthy()
  })
})
