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
