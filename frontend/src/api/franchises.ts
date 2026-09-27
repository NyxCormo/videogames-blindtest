// Forme du JSON renvoyé par GET /api/franchises (voir FranchiseResponse côté backend)
export type Franchise = {
  id: number
  name: string
}

export async function fetchFranchises(signal?: AbortSignal): Promise<Franchise[]> {
  const response = await fetch('/api/franchises', { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function createFranchise(name: string): Promise<Franchise> {
  const response = await fetch('/api/franchises', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!response.ok) {
    const body = await response.json()
    throw new Error(body.message ?? `Erreur ${response.status}`)
  }
  return response.json()
}
