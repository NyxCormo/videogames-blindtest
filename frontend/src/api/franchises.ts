import { request } from './http'

// Forme du JSON renvoyé par GET /api/franchises (voir FranchiseResponse côté backend)
export type Franchise = {
  id: number
  name: string
}

export async function fetchFranchises(signal?: AbortSignal): Promise<Franchise[]> {
  const response = await request('/api/franchises', { signal })
  return response.json()
}

export async function createFranchise(name: string): Promise<Franchise> {
  const response = await request('/api/franchises', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return response.json()
}
