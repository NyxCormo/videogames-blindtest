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

export async function createBlindtest(
    name: string,
    trackCount: number,
    difficulty: number
): Promise<Blindtest> {
    const response = await fetch(`/api/blindtests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, trackCount, difficulty }),
    })
    if (!response.ok) {
        const body = await response.json()
        throw new Error(body.message ?? `Erreur ${response.status}`)
    }
    return response.json()
}
