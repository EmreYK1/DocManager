import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import Modal from './Modal.jsx'

describe('Modal', () => {
  afterEach(cleanup)

  it('schließt per ✕, Escape und Klick auf den Hintergrund', () => {
    const onClose = vi.fn()
    const { container } = render(
      <Modal title="Test" onClose={onClose}>
        Inhalt
      </Modal>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Schließen' }))
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.mouseDown(container.querySelector('.modal-backdrop'))

    expect(onClose).toHaveBeenCalledTimes(3)
  })

  it('lässt sich nicht schließen, solange eine Aktion läuft (busy), und zeigt das an', () => {
    const onClose = vi.fn()
    const { container } = render(
      <Modal title="Test" onClose={onClose} busy>
        Inhalt
      </Modal>,
    )

    const closeButton = screen.getByRole('button', { name: 'Schließen' })
    fireEvent.click(closeButton)
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.mouseDown(container.querySelector('.modal-backdrop'))

    expect(closeButton.disabled).toBe(true)
    expect(onClose).not.toHaveBeenCalled()
  })
})
