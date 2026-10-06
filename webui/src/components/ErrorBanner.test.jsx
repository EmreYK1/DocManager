import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  afterEach(cleanup)

  it('zeigt nichts ohne Fehler', () => {
    const { container } = render(<ErrorBanner error={null} />)

    expect(container.textContent).toBe('')
  })

  it('zeigt die Meldung zum Fehler', () => {
    render(<ErrorBanner error={{ status: 0 }} />)

    expect(screen.getByRole('alert').textContent).toBe('Keine Verbindung zum Server')
  })
})
