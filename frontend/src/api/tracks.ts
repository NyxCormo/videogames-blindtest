// Forme du JSON renvoyé par GET /api/tracks (voir TrackResponse côté backend)
export type Track = {
  id: number
  name: string
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
