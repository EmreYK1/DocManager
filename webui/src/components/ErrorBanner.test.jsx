import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { ApiError } from '../api/httpClient.js'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  afterEach(cleanup)

  it('rendert nichts ohne Fehler', () => {
    const { container } = render(<ErrorBanner error={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('zeigt Statusmeldung und Feldfehler als Liste', () => {
    const error = new ApiError(400, 'Validierungsfehler', {
      content: 'Kommentarinhalt darf maximal 2000 Zeichen lang sein',
    })

    render(<ErrorBanner error={error} />)

    const alert = screen.getByRole('alert')
    expect(alert.textContent).toContain('Die Eingabe ist ungültig')
    expect(screen.getByText('Kommentarinhalt darf maximal 2000 Zeichen lang sein')).toBeTruthy()
    expect(alert.querySelectorAll('li')).toHaveLength(1)
  })

  it('zeigt nur die Statusmeldung ohne fields', () => {
    render(<ErrorBanner error={new ApiError(500, 'Boom')} />)

    expect(screen.getByRole('alert').textContent).toBe('Da ist etwas schiefgelaufen. Bitte versuche es später noch einmal.')
    expect(screen.queryByRole('list')).toBeNull()
  })
})
