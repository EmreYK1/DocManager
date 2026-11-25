import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import CommentForm from './CommentForm.jsx'

async function send(onSubmit, content) {
  render(<CommentForm onSubmit={onSubmit} />)
  fireEvent.change(screen.getByLabelText('Autor'), { target: { value: 'Max' } })
  fireEvent.change(screen.getByLabelText('Kommentar'), { target: { value: content } })
  await act(async () => fireEvent.click(screen.getByRole('button', { name: 'Kommentar senden' })))
}

describe('CommentForm', () => {
  afterEach(cleanup)

  it.each([
    ['leert das Formular nach dem Speichern', true, ''],
    ['behält die Eingabe, wenn das Speichern fehlschlägt', false, 'Toll'],
  ])('%s', async (_name, saved, remaining) => {
    const onSubmit = vi.fn().mockResolvedValue(saved)

    await send(onSubmit, 'Toll')

    expect(onSubmit).toHaveBeenCalledWith({ author: 'Max', content: 'Toll' })
    expect(screen.getByLabelText('Kommentar').value).toBe(remaining)
  })

  it('sendet keinen Kommentar, der nur aus Leerzeichen besteht', async () => {
    const onSubmit = vi.fn()

    await send(onSubmit, '   ')

    expect(onSubmit).not.toHaveBeenCalled()
  })
})
