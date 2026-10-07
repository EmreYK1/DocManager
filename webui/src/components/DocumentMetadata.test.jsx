import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import DocumentMetadata from './DocumentMetadata.jsx'

const document = {
  id: '1',
  filename: 'bericht.pdf',
  contentType: 'application/pdf',
  sizeBytes: 2048,
  status: 'OCR_DONE',
  uploadedAt: '2026-10-06T10:00:00Z',
  folderId: 'f1',
}

describe('DocumentMetadata', () => {
  afterEach(cleanup)

  it('zeigt Dateiname, Status, Größe und Ordner', () => {
    render(<DocumentMetadata document={document} folderName="Rechnungen" />)

    expect(screen.getByText('bericht.pdf')).toBeTruthy()
    expect(screen.getByText('OCR_DONE')).toBeTruthy()
    expect(screen.getByText('2048 Bytes')).toBeTruthy()
    expect(screen.getByText('Rechnungen')).toBeTruthy()
  })

  it('zeigt „–“ als Ordner, wenn keiner übergeben wird', () => {
    render(<DocumentMetadata document={document} folderName="–" />)

    expect(screen.getByText('–')).toBeTruthy()
  })
})
