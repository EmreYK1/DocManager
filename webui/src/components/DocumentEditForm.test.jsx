import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import DocumentEditForm from './DocumentEditForm.jsx'

const document = { id: '1', filename: 'test.pdf', status: 'UPLOADED', folderId: 'f1' }
const folders = [
  { id: 'f1', name: 'Rechnungen', parentId: null },
  { id: 'f2', name: 'Verträge', parentId: null },
]

describe('DocumentEditForm', () => {
  afterEach(cleanup)

  it('übergibt nur die geänderten Felder an onSave', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: 'neu.pdf' } })
    fireEvent.change(screen.getByLabelText('Ordner'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ filename: 'neu.pdf', clearFolder: true })
  })

  it('überschreibt einen zwischenzeitlich geänderten Status nicht mit dem alten', () => {
    const onSave = vi.fn()
    const pending = { ...document, status: 'OCR_PENDING' }
    const { rerender } = render(<DocumentEditForm document={pending} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: 'neu.pdf' } })
    // Während der Dialog offen ist, wird die Texterkennung fertig und die Seite lädt neu
    rerender(<DocumentEditForm document={{ ...pending, status: 'OCR_DONE' }} folders={folders} onSave={onSave} />)
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ filename: 'neu.pdf' })
  })

  it('sendet den Status, wenn der Nutzer ihn selbst ändert', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'FAILED' } })
    fireEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSave).toHaveBeenCalledWith({ status: 'FAILED' })
  })

  it('sperrt Speichern und Abbrechen, solange gespeichert wird', () => {
    render(<DocumentEditForm document={document} folders={folders} onSave={vi.fn()} onCancel={vi.fn()} busy />)

    expect(screen.getByRole('button', { name: 'Speichern' }).disabled).toBe(true)
    expect(screen.getByRole('button', { name: 'Abbrechen' }).disabled).toBe(true)
  })

  it('deaktiviert Speichern bei leerem Dateinamen', () => {
    const onSave = vi.fn()
    render(<DocumentEditForm document={document} folders={folders} onSave={onSave} />)

    fireEvent.change(screen.getByLabelText('Dateiname'), { target: { value: ' ' } })
    const button = screen.getByRole('button', { name: 'Speichern' })
    fireEvent.click(button)

    expect(button.disabled).toBe(true)
    expect(onSave).not.toHaveBeenCalled()
  })
})
