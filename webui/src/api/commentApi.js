import { request } from './httpClient.js'

export const createComment = (documentId, { author, content }) =>
    request(`/documents/${documentId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ author, content }),
    })

export const getComments = (documentId) => request(`/documents/${documentId}/comments`)

export const updateComment = (documentId, id, content) =>
    request(`/documents/${documentId}/comments/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
    })

export const deleteComment = (documentId, id) =>
    request(`/documents/${documentId}/comments/${id}`, { method: 'DELETE' })
