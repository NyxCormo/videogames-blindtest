import { describe, expect, it } from 'vitest'
import type { Room } from '../../api/rooms'
import { roomBanner } from './bannerContent'

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
  version: 0,
  phase: 'WAITING',
  players: [
    { listenerId: 1, name: 'Nyx', score: 0, done: false, roundPoints: null, gamesFound: 0, franchisesFound: 0, bonusesFound: 0 },
  ],
  currentTrack: null,
  playedTracks: [],
}
const other: Room = { ...room, code: 'EFGH', phase: 'PLAYING' }

describe('roomBanner', () => {
  it("n'affiche rien sans salle publique", () => {
    expect(roomBanner([], new Set())).toBeNull()
  })

  it('propose de rejoindre la seule salle ouverte', () => {
    expect(roomBanner([room], new Set())).toEqual({
      text: 'Salle publique ouverte : Stellar Blade (en attente, 1 joueur)',
      link: '/rooms/ABCD',
      linkLabel: 'Rejoindre',
      codes: ['ABCD'],
    })
  })

  it('renvoie vers la liste quand plusieurs salles sont ouvertes', () => {
    const banner = roomBanner([room, other], new Set())
    expect(banner?.text).toBe('2 salles publiques ouvertes')
    expect(banner?.link).toBe('/rooms')
  })

  it('oublie les salles masquées, mais pas les nouvelles', () => {
    expect(roomBanner([room], new Set(['ABCD']))).toBeNull()
    expect(roomBanner([room, other], new Set(['ABCD']))?.link).toBe('/rooms/EFGH')
  })
})
