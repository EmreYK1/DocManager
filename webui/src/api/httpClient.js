const API_BASE = '/api'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function readErrorMessage(response) {
  try {
    const body = await response.json()
    return body.message ?? response.statusText
  } catch {
    return response.statusText
  }
}

export async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new ApiError(0, 'Netzwerkfehler')
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }

  if (response.status === 204) {
    return null
  }
  const text = await response.text()
  return text ? JSON.parse(text) : null
}
