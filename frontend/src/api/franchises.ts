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
