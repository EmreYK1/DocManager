const API_BASE = '/api'

type FieldErrors = Record<string, string>

export class ApiError extends Error {
  readonly status: number
  readonly fields: FieldErrors | null

  constructor(status: number, message: string, fields: FieldErrors | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

async function readError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json()
    return new ApiError(response.status, body.message ?? response.statusText, body.fields ?? null)
  } catch {
    return new ApiError(response.status, response.statusText)
  }
}

export async function request<T = null>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    // Bei FormData setzt der Browser den Content-Type selbst (inkl. Trennmarke zwischen den Teilen).
    const headers: HeadersInit = options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch {
    throw new ApiError(0, 'Netzwerkfehler')
  }

  if (!response.ok) {
    throw await readError(response)
  }

  // Leer ist der Body z. B. bei 204
  const text = await response.text()
  return (text ? JSON.parse(text) : null) as T
}
