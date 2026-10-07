const dateFormatter = new Intl.DateTimeFormat('de-DE', {
  dateStyle: 'short',
  timeStyle: 'short',
})

export function formatDate(value) {
  return dateFormatter.format(new Date(value))
}
