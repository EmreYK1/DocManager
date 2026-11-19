import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import DocumentUploadForm from './DocumentUploadForm.jsx'

const folders = [{ id: 'f1', name: 'Rechnungen', parentId: null }]
const pdf = new File(['%PDF-1.4'], 'HelloWorld.pdf', { type: 'application/pdf' })
const txt = new File(['hallo'], 'notiz.txt', { type: 'text/plain' })

function chooseFile(file) {
  fireEvent.change(screen.getByLabelText('PDF-Datei'), { target: { files: [file] } })
}

describe('DocumentUploadForm', () => {
  afterEach(cleanup)

  it('hat ein Dateifeld, das nur PDF annimmt', () => {
    render(<DocumentUploadForm onSubmit={vi.fn()} />)

    expect(screen.getByLabelText('PDF-Datei').getAttribute('accept')).toBe('application/pdf,.pdf')
  })

  it('sperrt das Formular während des Hochladens und gibt es danach wieder frei', async () => {
    let finish
    const onSubmit = vi.fn(() => new Promise((resolve) => (finish = resolve)))
    const onCancel = vi.fn()
    render(<DocumentUploadForm onSubmit={onSubmit} onCancel={onCancel} />)

    chooseFile(pdf)
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))

    const busyButton = screen.getByRole('button', { name: 'Wird hochgeladen…' })
    expect(busyButton.disabled).toBe(true)
    expect(screen.getByRole('button', { name: 'Abbrechen' }).disabled).toBe(true)
    fireEvent.submit(busyButton.closest('form'))
    expect(onSubmit).toHaveBeenCalledTimes(1)

    await act(async () => finish(false))

    expect(screen.getByRole('button', { name: 'Hochladen' }).disabled).toBe(false)
    expect(screen.getByRole('button', { name: 'Abbrechen' }).disabled).toBe(false)
  })

  it('ist ohne Datei deaktiviert, zeigt aber noch keine Fehlermeldung', () => {
    render(<DocumentUploadForm onSubmit={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Hochladen' }).disabled).toBe(true)
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('ruft onSubmit mit Datei und Ordner auf', () => {
    const onSubmit = vi.fn()
    render(<DocumentUploadForm folders={folders} onSubmit={onSubmit} />)

    chooseFile(pdf)
    fireEvent.change(screen.getByLabelText('Ordner (optional)'), { target: { value: 'f1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))

    expect(onSubmit).toHaveBeenCalledWith({ file: pdf, folderId: 'f1' })
  })

  it('übergibt folderId null, wenn kein Ordner gewählt ist', () => {
    const onSubmit = vi.fn()
    render(<DocumentUploadForm onSubmit={onSubmit} />)

    chooseFile(pdf)
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))

    expect(onSubmit).toHaveBeenCalledWith({ file: pdf, folderId: null })
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

  it('erzeugt keinen unbehandelten Fehler, wenn onSubmit abgelehnt wird', async () => {
    const unhandled = vi.fn()
    process.on('unhandledRejection', unhandled)
    const onSubmit = vi.fn(() => Promise.reject(new Error('Boom')))
    render(<DocumentUploadForm onSubmit={onSubmit} />)

    chooseFile(pdf)
    fireEvent.click(screen.getByRole('button', { name: 'Hochladen' }))
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 20))
    })

    process.off('unhandledRejection', unhandled)
    expect(unhandled).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Hochladen' }).disabled).toBe(false)
  })

  it('übernimmt den vorgewählten Ordner', () => {
    render(<DocumentUploadForm folders={folders} initialFolderId="f1" onSubmit={vi.fn()} />)

    expect(screen.getByLabelText('Ordner (optional)').value).toBe('f1')
  })
})
