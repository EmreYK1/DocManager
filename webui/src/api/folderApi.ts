import { request } from './httpClient.ts'
import type { Folder, FolderInput } from './types.ts'

export const getFolders = () => request<Folder[]>('/folders')

export const createFolder = (folder: FolderInput) =>
  request<Folder>('/folders', { method: 'POST', body: JSON.stringify(folder) })

export const updateFolder = (id: string, folder: FolderInput) =>
  request<Folder>(`/folders/${id}`, { method: 'PUT', body: JSON.stringify(folder) })

export const deleteFolder = (id: string) => request(`/folders/${id}`, { method: 'DELETE' })
