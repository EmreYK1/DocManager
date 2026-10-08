import { request } from './httpClient.js'

export const getDocuments = () => request('/documents')

export const getDocument = (id) => request(`/documents/${id}`)

export const updateDocument = (id, changes) =>
  request(`/documents/${id}`, { method: 'PATCH', body: JSON.stringify(changes) })

export const deleteDocument = (id) => request(`/documents/${id}`, { method: 'DELETE' })

export const createDocument = (document) =>
  request('/documents', { method: 'POST', body: JSON.stringify(document) })
