import { request } from './http'
import type { Track } from './tracks'

// Forme du JSON renvoyé par GET /api/games (voir GameResponse côté backend)
export type Game = {
  id: number
  name: string
  franchiseId: number
  franchiseName: string
}

export async function fetchGames(signal?: AbortSignal): Promise<Game[]> {
  const response = await request('/api/games', { signal })
  return response.json()
}

export async function createGame(name: string, franchiseId: number): Promise<Game> {
  const response = await request('/api/games', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, franchiseId }),
  })
  return response.json()
}

export async function fetchGameTracks(gameId: number, signal?: AbortSignal): Promise<Track[]> {
  const response = await request(`/api/games/${gameId}/tracks`, { signal })
  return response.json()
}

export async function applyTagToGame(gameId: number, tagId: number): Promise<void> {
  await request(`/api/games/${gameId}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tagId }),
  })
}
