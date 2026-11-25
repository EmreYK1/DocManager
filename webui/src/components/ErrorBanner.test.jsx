import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { ApiError } from '../api/httpClient.ts'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  afterEach(cleanup)

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
})
