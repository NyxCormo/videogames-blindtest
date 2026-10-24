import { request } from './http'

// Forme du JSON renvoyé par GET /api/discover/next (voir DiscoverTrackResponse côté backend)
export type DiscoverTrack = {
  trackId: number | null
  audioLink: string | null
  franchiseName: string | null
  gameId: number | null
  gameName: string | null
  trackName: string | null
  finished: boolean
  voted: number
  remaining: number
  coverUrl: string | null
}

export async function fetchNextTrack(listenerId: number, signal?: AbortSignal): Promise<DiscoverTrack> {
  const response = await request(`/api/discover/next?listenerId=${listenerId}`, { signal })
  return response.json()
}

export async function setKnowledge(listenerId: number, trackId: number, knows: boolean): Promise<void> {
  await request(
    `/api/discover/knowledge?listenerId=${listenerId}&trackId=${trackId}&knows=${knows}`,
    { method: 'POST' },
  )
}

// Forme du JSON renvoyé par GET /api/discover/knowledge (voir KnowledgeEntryResponse côté backend)
export type KnowledgeEntry = {
  trackId: number
  knows: boolean
}

export async function fetchKnowledge(listenerId: number, signal?: AbortSignal): Promise<KnowledgeEntry[]> {
  const response = await request(`/api/discover/knowledge?listenerId=${listenerId}`, { signal })
  return response.json()
}

export async function deleteKnowledge(listenerId: number, trackId: number): Promise<void> {
  await request(`/api/discover/knowledge?listenerId=${listenerId}&trackId=${trackId}`, {
    method: 'DELETE',
  })
}
