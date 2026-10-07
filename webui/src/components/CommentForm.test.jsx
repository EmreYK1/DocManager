import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import CommentForm from './CommentForm.jsx'

describe('CommentForm', () => {
    afterEach(cleanup)

    it('ruft onSubmit mit Autor und Inhalt auf', () => {
        const onSubmit = vi.fn().mockResolvedValue()  // gibt Promise zurück
      
        render(<CommentForm onSubmit={onSubmit} />)
      
        fireEvent.change(screen.getByLabelText('Author'), { target: { value: 'Max' } })
        fireEvent.change(screen.getByLabelText('Content'), { target: { value: 'Toller Kommentar' } })
        fireEvent.click(screen.getByRole('button', { name: 'Kommentar senden' }))
      
        expect(onSubmit).toHaveBeenCalledWith({ author: 'Max', content: 'Toller Kommentar' })
      })
    });