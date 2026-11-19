import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import StatusBadge from './StatusBadge.jsx'

describe('StatusBadge', () => {
  afterEach(cleanup)

  it.each([
    ['UPLOADED', 'Hochgeladen'],
    ['OCR_PENDING', 'Text wird erkannt…'],
    ['OCR_DONE', 'Durchsuchbar'],
    ['SUMMARY_DONE', 'Zusammenfassung fertig'],
    ['FAILED', 'Texterkennung fehlgeschlagen'],
  ])('zeigt für %s den Klartext „%s“ statt des technischen Werts', (status, label) => {
    render(<StatusBadge status={status} />)

    expect(screen.getByText(label)).toBeTruthy()
    expect(screen.queryByText(status)).toBeNull()
  })

  it('erklärt den Status im Tooltip', () => {
    render(<StatusBadge status="OCR_DONE" />)

    expect(screen.getByText('Durchsuchbar').getAttribute('title')).toMatch(/Suche/)
  })

  it('zeigt bei unbekanntem Status den Rohwert', () => {
    render(<StatusBadge status="NEU" />)

    expect(screen.getByText('NEU')).toBeTruthy()
  })
})
