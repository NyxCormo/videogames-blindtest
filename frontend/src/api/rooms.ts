import type { Reveal } from './blindtests'
import { request } from './http'

export type RoomVisibility = 'PUBLIC' | 'PRIVATE'
export type RoomMode = 'MULTI' | 'PRESENTATION'
export type ScoringMode = 'SPEED' | 'FIRST_COME'
export type RoomPhase = 'WAITING' | 'PLAYING' | 'REVEAL' | 'FINISHED'

// Forme du JSON renvoyé par /api/rooms (voir RoomResponse côté backend)
export type RoomPlayer = {
  listenerId: number
  name: string
  score: number
  done: boolean
  roundPoints: number | null
  present: boolean
  gamesFound: number
  franchisesFound: number
  bonusesFound: number
  tracksHeard: number
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
  hostName: string
  visibility: RoomVisibility
  mode: RoomMode
  playersCanSeek: boolean
  scoringMode: ScoringMode
  answerSeconds: number
  maxAttempts: number
  createdAt: string
  version: number
  phase: RoomPhase
  players: RoomPlayer[]
  currentTrack: CurrentTrack | null
  playedTracks: Reveal[]
}

export type RoomSettings = {
  blindtestId: number
  visibility: RoomVisibility
  mode: RoomMode
  playersCanSeek: boolean
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

export async function sendPresence(code: string, listenerId: number): Promise<void> {
  await request(`/api/rooms/${code}/presence?listenerId=${listenerId}`, { method: 'POST' })
}

export function leaveRoom(code: string, listenerId: number): void {
  navigator.sendBeacon(`/api/rooms/${code}/leave?listenerId=${listenerId}`)
}

export async function revealInRoom(code: string, listenerId: number): Promise<Room> {
  const response = await request(`/api/rooms/${code}/reveal?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}

export async function nextInRoom(code: string, listenerId: number): Promise<Room> {
  const response = await request(`/api/rooms/${code}/next?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}

// Forme du JSON renvoyé par /api/rooms/{code}/round et par les réponses (voir RoomRound côté backend)
export type RoomRound = {
  position: number
  trackId: number | null
  score: number
  roundPoints: number
  attemptsRemaining: number
  foundFranchiseId: number | null
  gameFound: boolean
  bonusFound: boolean
  done: boolean
}

export async function fetchRound(code: string, listenerId: number, signal?: AbortSignal): Promise<RoomRound> {
  const response = await request(`/api/rooms/${code}/round?listenerId=${listenerId}`, { signal })
  return response.json()
}

export async function guessInRoom(code: string, listenerId: number, gameId: number, trackId: number | null): Promise<RoomRound> {
  const response = await request(`/api/rooms/${code}/guess?listenerId=${listenerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ gameId, trackId }),
  })
  return response.json()
}

export async function guessFranchiseInRoom(code: string, listenerId: number, franchiseId: number): Promise<RoomRound> {
  const response = await request(`/api/rooms/${code}/guess-franchise?listenerId=${listenerId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ franchiseId }),
  })
  return response.json()
}

export async function passInRoom(code: string, listenerId: number): Promise<RoomRound> {
  const response = await request(`/api/rooms/${code}/pass?listenerId=${listenerId}`, { method: 'POST' })
  return response.json()
}
