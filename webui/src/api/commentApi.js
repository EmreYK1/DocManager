import { request } from './httpClient.js'

export const getComments = (documentId) => request(`/documents/${documentId}/comments`)
