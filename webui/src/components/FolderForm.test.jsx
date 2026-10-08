import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import FolderForm from './FolderForm.jsx'

const folders = [
  { id: 'f1', name: 'Rechnungen', parentId: null },
  { id: 'f2', name: 'Verträge', parentId: null },
]

describe('FolderForm', () => {
  afterEach(cleanup)

  it('ruft onSubmit genau einmal mit Name und gewählter Eltern-ID auf', () => {
    const onSubmit = vi.fn()
    render(<FolderForm folders={folders} onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: ' Unterordner ' } })
    fireEvent.change(screen.getByLabelText('Elternordner'), { target: { value: 'f1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ordner anlegen' }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    expect(onSubmit).toHaveBeenCalledWith({ name: 'Unterordner', parentId: 'f1' })
  })

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

  it('blendet ausgeschlossene Ordner im Elternordner-Dropdown aus', () => {
    render(<FolderForm folders={folders} excludeIds={new Set(['f1'])} onSubmit={vi.fn()} />)

    expect(screen.queryByRole('option', { name: 'Rechnungen' })).toBeNull()
    expect(screen.getByRole('option', { name: 'Verträge' })).toBeTruthy()
  })
})
