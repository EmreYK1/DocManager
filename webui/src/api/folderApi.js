import { request } from './httpClient.js'

export const getFolders = () => request('/folders')

export const createFolder = (folder) =>
  request('/folders', { method: 'POST', body: JSON.stringify(folder) })

export const updateFolder = (id, folder) =>
  request(`/folders/${id}`, { method: 'PUT', body: JSON.stringify(folder) })

export const deleteFolder = (id) => request(`/folders/${id}`, { method: 'DELETE' })
