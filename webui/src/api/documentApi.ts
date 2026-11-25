import { request } from './httpClient.ts'
import type { DocumentChanges, DocumentDetails, DocumentSummary } from './types.ts'

export const getDocuments = () => request<DocumentSummary[]>('/documents')

export const getDocument = (id: string) => request<DocumentDetails>(`/documents/${id}`)

export const updateDocument = (id: string, changes: DocumentChanges) =>
  request<DocumentDetails>(`/documents/${id}`, { method: 'PATCH', body: JSON.stringify(changes) })

export const deleteDocument = (id: string) => request(`/documents/${id}`, { method: 'DELETE' })

export const uploadDocument = (file: File, folderId: string | null) => {
  const body = new FormData()
  // Das Backend akzeptiert nur application/pdf. Meldet der Browser für eine .pdf einen
  // leeren oder abweichenden Typ, wird er hier korrigiert.
  const pdf = file.type === 'application/pdf' ? file : new File([file], file.name, { type: 'application/pdf' })
  body.append('file', pdf)
  if (folderId) body.append('folderId', folderId)
  return request<DocumentDetails>('/documents/upload', { method: 'POST', body })
}

export const searchDocuments = (query: string) =>
  request<DocumentSummary[]>(`/documents/search?q=${encodeURIComponent(query)}`)
