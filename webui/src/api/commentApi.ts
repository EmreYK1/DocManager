import { request } from './httpClient.ts'
import type { Comment, CommentInput } from './types.ts'

export const createComment = (documentId: string, comment: CommentInput) =>
  request<Comment>(`/documents/${documentId}/comments`, { method: 'POST', body: JSON.stringify(comment) })

export const getComments = (documentId: string) => request<Comment[]>(`/documents/${documentId}/comments`)

export const updateComment = (documentId: string, id: string, content: string) =>
  request<Comment>(`/documents/${documentId}/comments/${id}`, { method: 'PUT', body: JSON.stringify({ content }) })

export const deleteComment = (documentId: string, id: string) =>
  request(`/documents/${documentId}/comments/${id}`, { method: 'DELETE' })
