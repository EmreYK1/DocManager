import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import RecognizedText from './RecognizedText.jsx'

describe('RecognizedText', () => {
  afterEach(cleanup)

  it('zeigt den erkannten Text', () => {
    render(<RecognizedText status="OCR_DONE" text="Hello World" />)

    expect(screen.getByText('Hello World')).toBeTruthy()
  })

  it('erklärt, dass der Text noch erkannt wird', () => {
    render(<RecognizedText status="OCR_PENDING" text={null} />)

    expect(screen.getByText(/wird gerade erkannt/)).toBeTruthy()
  })

  it('erklärt einen Fehlschlag', () => {
    render(<RecognizedText status="FAILED" text={null} />)

    expect(screen.getByText(/konnte nicht erkannt werden/)).toBeTruthy()
  })

  it('erklärt, wenn kein Text gefunden wurde', () => {
    render(<RecognizedText status="OCR_DONE" text="  " />)

    expect(screen.getByText(/kein Text gefunden/)).toBeTruthy()
  })
})
