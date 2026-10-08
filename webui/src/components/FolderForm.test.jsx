import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import FolderForm from './FolderForm.jsx'

const folders = [
  { id: 'f1', name: 'Rechnungen', parentId: null },
  { id: 'f2', name: 'Verträge', parentId: null },
]

describe('FolderForm', () => {
  afterEach(cleanup)

  it('ruft onSubmit mit Name und ohne Elternordner auf', () => {
    const onSubmit = vi.fn()
    render(<FolderForm folders={folders} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Steuern' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ordner anlegen' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Steuern', parentId: null })
  })

  it('ruft onSubmit mit der gewählten Eltern-ID auf', () => {
    const onSubmit = vi.fn()
    render(<FolderForm folders={folders} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Unterordner' } })
    fireEvent.change(screen.getByLabelText('Elternordner'), { target: { value: 'f1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ordner anlegen' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Unterordner', parentId: 'f1' })
  })

  it('zeigt alle übergebenen Ordner als Auswahlmöglichkeit für den Elternordner', () => {
    render(<FolderForm folders={folders} onSubmit={vi.fn()} />)

    expect(screen.getByRole('option', { name: 'Rechnungen' })).toBeTruthy()
    expect(screen.getByRole('option', { name: 'Verträge' })).toBeTruthy()
  })

  it('leert die Felder nach dem Absenden', () => {
    render(<FolderForm folders={folders} onSubmit={vi.fn()} />)

    const nameInput = screen.getByLabelText('Name')
    fireEvent.change(nameInput, { target: { value: 'Steuern' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ordner anlegen' }))

    expect(nameInput.value).toBe('')
  })

  it('ruft keine API auf, sondern nur die übergebene onSubmit-Funktion', () => {
    const onSubmit = vi.fn()
    render(<FolderForm folders={folders} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Steuern' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ordner anlegen' }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
  })
})

describe('FolderForm Validierung', () => {
  afterEach(cleanup)

  it('deaktiviert den Button bei leerem Namen und sendet nichts', () => {
    const onSubmit = vi.fn()
    render(<FolderForm folders={folders} onSubmit={onSubmit} />)

    const button = screen.getByRole('button', { name: 'Ordner anlegen' })
    expect(button.disabled).toBe(true)

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: '   ' } })
    expect(button.disabled).toBe(true)
    expect(screen.getByRole('alert').textContent).toBe('Ordnername darf nicht leer sein.')

    fireEvent.click(button)
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
