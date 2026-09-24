// Forme du JSON renvoyé par GET /api/tracks (voir TrackResponse côté backend)
import type { Tag } from './tags'

export type Track = {
  id: number
  name: string
  gameId: number
  gameName: string
  franchiseName: string
  khinsiderLink: string | null
  youtubeLink: string | null
  audioLink: string | null
}

export async function fetchTracks(signal?: AbortSignal): Promise<Track[]> {
  const response = await fetch('/api/tracks', { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export function hasSource(track: Track): boolean {
  return track.khinsiderLink !== null || track.youtubeLink !== null
}

export async function fetchTrack(id: number, signal?: AbortSignal): Promise<Track> {
  const response = await fetch(`/api/tracks/${id}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function fetchTrackTags(id: number, signal?: AbortSignal): Promise<Tag[]> {
  const response = await fetch(`/api/tracks/${id}/tags`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function addTrackTag(id: number, tagId: number): Promise<void> {
  const response = await fetch(`/api/tracks/${id}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tagId }),
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

export async function removeTrackTag(id: number, tagId: number): Promise<void> {
  const response = await fetch(`/api/tracks/${id}/tags/${tagId}`, { method: 'DELETE' })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}
