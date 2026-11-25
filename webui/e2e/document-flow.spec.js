// Der Hauptweg im echten Browser gegen den laufenden Docker-Stack (docker compose up -d --build).
import { readFileSync } from 'node:fs'
import { expect, test } from '@playwright/test'

const pdf = readFileSync(new URL('../../docs/samples/HelloWorld.pdf', import.meta.url))

test('hochladen, Text erkennen, suchen, umbenennen und löschen', async ({ page }) => {
  const name = `e2e-${Date.now()}.pdf`
  const renamed = `umbenannt-${Date.now()}.pdf`
  const search = async (term) => {
    await page.getByLabel('Dokumente durchsuchen').fill(term)
    await page.getByRole('button', { name: 'Suchen', exact: true }).click()
  }
  // Der Suchindex wird kurz nach einer Änderung aktualisiert, deshalb wird die Suche wiederholt
  const expectSearchHit = (term, filename) =>
    expect(async () => {
      await search(term)
      await expect(page.getByRole('link', { name: filename })).toBeVisible({ timeout: 1000 })
    }).toPass({ timeout: 20_000 })

  await page.goto('/')
  await page.getByRole('button', { name: 'PDF hochladen' }).click()
  await page.getByLabel('PDF-Datei').setInputFiles({ name, mimeType: 'application/pdf', buffer: pdf })
  await page.getByRole('button', { name: 'Hochladen', exact: true }).click()

  const row = page.getByRole('row').filter({ hasText: name })
  await expect(row.getByText('Durchsuchbar')).toBeVisible({ timeout: 60_000 })

  await expectSearchHit('Helo', name)
  await page.getByRole('link', { name }).click()
  await expect(page.getByText('Hello World')).toBeVisible()

  await page.getByRole('button', { name: 'Bearbeiten', exact: true }).click()
  await page.getByLabel('Dateiname').fill(renamed)
  await page.getByRole('button', { name: 'Speichern' }).click()
  await expect(page.getByRole('heading', { name: renamed })).toBeVisible()

  await page.getByRole('link', { name: 'Zurück zur Übersicht' }).click()
  await expectSearchHit(renamed.replace('.pdf', ''), renamed)

  await page.getByRole('button', { name: `${renamed} löschen` }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Löschen' }).click()
  await expect(page.getByRole('link', { name: renamed })).toHaveCount(0)
})
