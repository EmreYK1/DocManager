import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import CommentList from './CommentList.jsx'

const comments = [
  { id: '1', author: 'Alice', content: 'Erster Kommentar', createdAt: '2026-10-06T10:00:00Z' },
  { id: '2', author: 'Bob', content: 'Zweiter Kommentar', createdAt: '2026-10-07T08:30:00Z' },
]

describe('CommentList', () => {
  afterEach(cleanup)

  it('zeigt Autor und Inhalt für jeden Kommentar', () => {
    render(<CommentList comments={comments} />)

    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(items[0].textContent).toContain('Alice')
    expect(items[0].textContent).toContain('Erster Kommentar')
    expect(items[1].textContent).toContain('Bob')
    expect(items[1].textContent).toContain('Zweiter Kommentar')
  })

  it('zeigt „Keine Kommentare“, wenn die Liste leer ist', () => {
    render(<CommentList comments={[]} />)

    expect(screen.getByText('Keine Kommentare')).toBeTruthy()
  })
})
