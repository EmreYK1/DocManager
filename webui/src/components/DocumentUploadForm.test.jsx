import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import DocumentUploadForm from './DocumentUploadForm.jsx'

const folders = [{ id: 'f1', name: 'Rechnungen', parentId: null }]
const pdf = new File(['%PDF-1.4'], 'HelloWorld.pdf', { type: 'application/pdf' })
const txt = new File(['hallo'], 'notiz.txt', { type: 'text/plain' })

function chooseFile(file) {
  fireEvent.change(screen.getByLabelText('PDF-Datei'), { target: { files: [file] } })
}

describe('DocumentUploadForm', () => {
  afterEach(cleanup)

  it('ist gesperrt, solange hochgeladen wird', () => {
    const onSubmit = vi.fn()
    const { rerender } = render(<DocumentUploadForm onSubmit={onSubmit} />)

    chooseFile(pdf)
    rerender(<DocumentUploadForm onSubmit={onSubmit} busy />)

    const busyButton = screen.getByRole('button', { name: 'Wird hochgeladen…' })
    expect(busyButton.disabled).toBe(true)
    expect(screen.getByLabelText('PDF-Datei').disabled).toBe(true)
    fireEvent.submit(busyButton.closest('form'))
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('ruft onSubmit mit Datei und Ordner auf', () => {
    const onSubmit = vi.fn()
    render(<DocumentUploadForm folders={folders} onSubmit={onSubmit} />)

    chooseFile(pdf)
    fireEvent.change(screen.getByLabelText('Ordner (optional)'), { target: { value: 'f1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))

    expect(onSubmit).toHaveBeenCalledWith({ file: pdf, folderId: 'f1' })
  })

  it('deaktiviert den Button und zeigt eine Meldung bei einer .txt-Datei', () => {
    const onSubmit = vi.fn()
    render(<DocumentUploadForm onSubmit={onSubmit} />)

    chooseFile(txt)

    const button = screen.getByRole('button', { name: 'Hochladen' })
    expect(button.disabled).toBe(true)
    expect(screen.getByRole('alert').textContent).toBe('Datei muss eine PDF-Datei sein.')
    fireEvent.submit(button.closest('form'))
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
