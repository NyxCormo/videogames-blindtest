// Forme du JSON renvoyé par GET /api/blindtests (voir BlindtestResponse côté backend)
export type Blindtest = {
  id: number
  name: string
  difficulty: number
  createdAt: string
}

export async function fetchBlindtests(signal?: AbortSignal): Promise<Blindtest[]> {
  const response = await fetch('/api/blindtests', { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}
