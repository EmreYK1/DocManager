import { request } from './httpClient.js'

export const getFolders = () => request('/folders')
