// Forme du JSON renvoyé par GET /api/tracks (voir TrackResponse côté backend)
import type { Tag } from './tags'
import { request } from './http'

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
  const response = await request('/api/tracks', { signal })
  return response.json()
}

export async function createTrack(name: string, gameId: number): Promise<Track> {
  const response = await request('/api/tracks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, gameId }),
  })
  return response.json()
}

export async function addKhinsiderLink(id: number, link: string): Promise<Track> {
  const response = await request(`/api/tracks/${id}/khinsider-link`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ link }),
  })
  return response.json()
}

export async function addYoutubeLink(id: number, link: string): Promise<Track> {
  const response = await request(`/api/tracks/${id}/youtube-link`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ link }),
  })
  return response.json()
}

export function hasSource(track: Track): boolean {
  return track.khinsiderLink !== null || track.youtubeLink !== null
}

export async function fetchTrack(id: number, signal?: AbortSignal): Promise<Track> {
  const response = await request(`/api/tracks/${id}`, { signal })
  return response.json()
}

export async function fetchTrackTags(id: number, signal?: AbortSignal): Promise<Tag[]> {
  const response = await request(`/api/tracks/${id}/tags`, { signal })
  return response.json()
}

export async function addTrackTag(id: number, tagId: number): Promise<void> {
  await request(`/api/tracks/${id}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tagId }),
  })
}

export async function removeTrackTag(id: number, tagId: number): Promise<void> {
  await request(`/api/tracks/${id}/tags/${tagId}`, { method: 'DELETE' })
}
