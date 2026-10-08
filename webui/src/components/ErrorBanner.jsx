import { errorMessage } from '../api/errorMessages.js'

export default function ErrorBanner({ error }) {
  if (!error) {
    return null
  }

  return (
    <div role="alert" className="banner banner-error">
      {errorMessage(error)}
    </div>
  )
}
