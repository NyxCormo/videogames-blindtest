// Forme du JSON renvoyé par GET /api/discover/next (voir DiscoverTrackResponse côté backend)
export type DiscoverTrack = {
  trackId: number | null
  audioLink: string | null
  franchiseName: string | null
  gameId: number | null
  gameName: string | null
  trackName: string | null
  finished: boolean
}

export async function fetchNextTrack(listenerId: number, signal?: AbortSignal): Promise<DiscoverTrack> {
  const response = await fetch(`/api/discover/next?listenerId=${listenerId}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function setKnowledge(listenerId: number, trackId: number, knows: boolean): Promise<void> {
  const response = await fetch(
    `/api/discover/knowledge?listenerId=${listenerId}&trackId=${trackId}&knows=${knows}`,
    { method: 'POST' },
  )
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

// Forme du JSON renvoyé par GET /api/discover/knowledge (voir KnowledgeEntryResponse côté backend)
export type KnowledgeEntry = {
  trackId: number
  knows: boolean
}

export async function fetchKnowledge(listenerId: number, signal?: AbortSignal): Promise<KnowledgeEntry[]> {
  const response = await fetch(`/api/discover/knowledge?listenerId=${listenerId}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function deleteKnowledge(listenerId: number, trackId: number): Promise<void> {
  const response = await fetch(`/api/discover/knowledge?listenerId=${listenerId}&trackId=${trackId}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}
