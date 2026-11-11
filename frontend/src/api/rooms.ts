import type { Reveal } from './blindtests'
import { request } from './http'

export type RoomVisibility = 'PUBLIC' | 'PRIVATE'
export type SoundMode = 'EVERY_DEVICE' | 'MAIN_SCREEN'
export type ScoringMode = 'SPEED' | 'FIRST_COME'
export type RoomPhase = 'WAITING' | 'PLAYING' | 'REVEAL' | 'FINISHED'

// Forme du JSON renvoyé par /api/rooms (voir RoomResponse côté backend)
export type RoomPlayer = {
  listenerId: number
  name: string
  score: number
  done: boolean
  roundPoints: number | null
}

export type CurrentTrack = {
  position: number
  trackId: number
  audioLink: string | null
  startsAt: string
  endsAt: string
  reveal: Reveal | null
}

export type Room = {
  code: string
  blindtestId: number
  blindtestName: string
  trackCount: number
  hostId: number
  visibility: RoomVisibility
  soundMode: SoundMode
  scoringMode: ScoringMode
  answerSeconds: number
  maxAttempts: number
  createdAt: string
  version: number
  phase: RoomPhase
  players: RoomPlayer[]
  currentTrack: CurrentTrack | null
}

export type RoomSettings = {
  blindtestId: number
  visibility: RoomVisibility
  soundMode: SoundMode
  scoringMode: ScoringMode
  answerSeconds: number
}

export async function fetchPublicRooms(signal?: AbortSignal): Promise<Room[]> {
  const response = await request('/api/rooms', { signal })
  return response.json()
}

export async function createRoom(listenerId: number, settings: RoomSettings): Promise<Room> {
  const response = await request(`/api/rooms?listenerId=${listenerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  })
  return response.json()
}

export async function joinRoom(code: string, listenerId: number): Promise<Room> {
  const response = await request(`/api/rooms/${code}/players?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}

export async function startRoom(code: string, listenerId: number): Promise<Room> {
  const response = await request(`/api/rooms/${code}/start?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}

export async function stopRoom(code: string, listenerId: number): Promise<Room> {
  const response = await request(`/api/rooms/${code}/stop?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}
