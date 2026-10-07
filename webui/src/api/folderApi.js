import { request } from './httpClient.js'

export const getFolders = () => request('/folders')

export const createFolder = (folder) =>
  request('/folders', { method: 'POST', body: JSON.stringify(folder) })
