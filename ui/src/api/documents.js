const BASE_URL = '/api'

export async function getAllDocuments() {
    const res = await fetch(`${BASE_URL}/documents`)
    return res.json()
}