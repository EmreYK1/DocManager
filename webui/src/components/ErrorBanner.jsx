import { errorMessage } from '../api/errorMessages.ts'

export default function ErrorBanner({ error }) {
  if (!error) {
    return null
  }

  const fieldEntries = error.fields ? Object.entries(error.fields) : []

  return (
    <div role="alert" className="banner banner-error">
      <p className="banner-message">{errorMessage(error)}</p>
      {fieldEntries.length > 0 && (
        <ul className="banner-fields">
          {fieldEntries.map(([field, message]) => (
            <li key={field}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
