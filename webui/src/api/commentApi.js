import { request } from './httpClient.js'

export const createComment = (documentId, { author, content }) =>
    request(`/documents/${documentId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ author, content }),
    })

export const getComments = (documentId) => request(`/documents/${documentId}/comments`)
