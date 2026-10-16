import { request } from './http'

// Forme du JSON renvoyé par GET /api/stats (voir StatsResponse côté backend)
export type Stats = {
  tracks: number
  playableTracks: number
  games: number
  franchises: number
  listeners: number
  votes: number
}

export async function fetchStats(signal?: AbortSignal): Promise<Stats> {
  const response = await request('/api/stats', { signal })
  return response.json()
}

// Forme du JSON renvoyé par GET /api/stats/listeners/{id} (voir ListenerStatsResponse côté backend)
export type ListenerStats = {
  votes: number
  known: number
  remaining: number
}

export async function fetchListenerStats(listenerId: number, signal?: AbortSignal): Promise<ListenerStats> {
  const response = await request(`/api/stats/listeners/${listenerId}`, { signal })
  return response.json()
}
