import { describe, expect, it } from 'vitest'
import type { Room } from '../../api/rooms'
import { latestRoom } from './latestRoom'

const room: Room = {
  code: 'ABCD',
  blindtestId: 1,
  blindtestName: 'Stellar Blade',
  trackCount: 4,
  hostId: 1,
  hostName: 'Nyx',
  visibility: 'PUBLIC',
  mode: 'MULTI',
  playersCanSeek: true,
  scoringMode: 'SPEED',
  answerSeconds: 30,
  maxAttempts: 5,
  createdAt: '2026-11-11T20:00:00Z',
  version: 3,
  phase: 'WAITING',
  players: [],
  playedTracks: [],
  currentTrack: null,
}

describe('latestRoom', () => {
  it('garde le nouvel état', () => {
    const next = { ...room, version: 4, phase: 'PLAYING' as const }
    expect(latestRoom(room, next)).toBe(next)
    expect(latestRoom(null, room)).toBe(room)
  })

  it('ignore un état plus ancien arrivé en retard', () => {
    expect(latestRoom(room, { ...room, version: 2 })).toBe(room)
  })

  it("accepte l'état d'une autre salle", () => {
    const other = { ...room, code: 'EFGH', version: 0 }
    expect(latestRoom(room, other)).toBe(other)
  })
})
