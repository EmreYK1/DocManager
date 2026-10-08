import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import DocumentCreateForm from './DocumentCreateForm.jsx'

const folders = [{ id: 'f1', name: 'Rechnungen', parentId: null }]

describe('DocumentCreateForm', () => {
  afterEach(cleanup)

  it('ruft onSubmit mit den Metadaten auf', () => {
    const onSubmit = vi.fn()
    render(<DocumentCreateForm folders={folders} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: ' bericht.pdf ' } })
    fireEvent.change(screen.getByLabelText('Größe (Bytes)'), { target: { value: '2048' } })
    fireEvent.change(screen.getByLabelText('Ordner'), { target: { value: 'f1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dokument anlegen' }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit).toHaveBeenCalledWith({
      filename: 'bericht.pdf',
      contentType: 'application/pdf',
      sizeBytes: 2048,
      folderId: 'f1',
    })
  })

  it('deaktiviert den Button bei leerem Dateinamen oder ungültiger Größe', () => {
    render(<DocumentCreateForm onSubmit={vi.fn()} />)
    const button = screen.getByRole('button', { name: 'Dokument anlegen' })
    expect(button.disabled).toBe(true)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: 'a.pdf' } })
    expect(button.disabled).toBe(false)

    fireEvent.change(screen.getByLabelText('Größe (Bytes)'), { target: { value: '-5' } })
    expect(button.disabled).toBe(true)
  })
})
