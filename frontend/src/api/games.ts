import type { Track } from './tracks'

export async function fetchGameTracks(gameId: number, signal?: AbortSignal): Promise<Track[]> {
  const response = await fetch(`/api/games/${gameId}/tracks`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function applyTagToGame(gameId: number, tagId: number): Promise<void> {
  const response = await fetch(`/api/games/${gameId}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tagId }),
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}
