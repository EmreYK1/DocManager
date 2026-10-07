import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import DocumentEditForm from './DocumentEditForm.jsx'

const document = {
  id: '1',
  filename: 'test.pdf',
  status: 'UPLOADED',
  folderId: 'f1',
}

const folders = [
  { id: 'f1', name: 'Rechnungen', parentId: null },
  { id: 'f2', name: 'Verträge', parentId: null },
]

describe('DocumentEditForm', () => {
  afterEach(cleanup)

  it('übergibt nur den geänderten Dateinamen an onSave', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: 'neu.pdf' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ filename: 'neu.pdf' })
  })

  it('übergibt nur den geänderten Status an onSave', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'OCR_DONE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ status: 'OCR_DONE' })
  })

  it('übergibt die neue folderId, wenn ein anderer Ordner gewählt wird', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Ordner'), { target: { value: 'f2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ folderId: 'f2' })
  })

  it('übergibt clearFolder, wenn der Ordner entfernt wird', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Ordner'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ clearFolder: true })
  })

  it('übergibt ein leeres Objekt, wenn nichts geändert wurde', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({})
  })
})
