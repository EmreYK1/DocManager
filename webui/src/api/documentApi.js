import { request } from './httpClient.js'

export const getDocuments = () => request('/documents')

export const getDocument = (id) => request(`/documents/${id}`)
