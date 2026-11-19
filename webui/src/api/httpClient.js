const API_BASE = '/api'

export class ApiError extends Error {
  constructor(status, message, fields = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

async function readErrorBody(response) {
  try {
    const body = await response.json()
    return {
      message: body.message ?? response.statusText,
      fields: body.fields ?? null,
    }
  } catch {
    return { message: response.statusText, fields: null }
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
    const { message, fields } = await readErrorBody(response)
    throw new ApiError(response.status, message, fields)
  }

  if (response.status === 204) {
    return null
  }
  const text = await response.text()
  return text ? JSON.parse(text) : null
}
