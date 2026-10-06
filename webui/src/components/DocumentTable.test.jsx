import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import DocumentTable from './DocumentTable.jsx'

const documents = [
  { id: '1', filename: 'bericht.pdf', status: 'UPLOADED', uploadedAt: '2026-10-06T10:00:00Z' },
  { id: '2', filename: 'rechnung.pdf', status: 'OCR_DONE', uploadedAt: '2026-10-05T08:30:00Z' },
]

describe('DocumentTable', () => {
  afterEach(cleanup)

  it('zeigt Dateiname und Status pro Zeile', () => {
    render(<DocumentTable documents={documents} />)

    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3)
    expect(rows[1].textContent).toContain('bericht.pdf')
    expect(rows[1].textContent).toContain('UPLOADED')
    expect(rows[2].textContent).toContain('rechnung.pdf')
    expect(rows[2].textContent).toContain('OCR_DONE')
  })

  it('zeigt einen Hinweis, wenn keine Dokumente vorhanden sind', () => {
    render(<DocumentTable documents={[]} />)

    expect(screen.getByText('Keine Dokumente vorhanden.')).toBeTruthy()
  })
})
