import { request } from './httpClient.js'

export const getDocuments = () => request('/documents')

export const getDocument = (id) => request(`/documents/${id}`)

export const updateDocument = (id, changes) =>
  request(`/documents/${id}`, { method: 'PATCH', body: JSON.stringify(changes) })

export const deleteDocument = (id) => request(`/documents/${id}`, { method: 'DELETE' })

export const uploadDocument = (file, folderId) => {
  const body = new FormData()
  // Das Backend akzeptiert nur application/pdf. Meldet der Browser für eine .pdf einen
  // leeren oder abweichenden Typ, wird er hier korrigiert.
  const pdf = file.type === 'application/pdf' ? file : new File([file], file.name, { type: 'application/pdf' })
  body.append('file', pdf)
  if (folderId) body.append('folderId', folderId)
  return request('/documents/upload', { method: 'POST', body })
}

export const searchDocuments = (query) =>
  request(`/documents/search?q=${encodeURIComponent(query)}`)
