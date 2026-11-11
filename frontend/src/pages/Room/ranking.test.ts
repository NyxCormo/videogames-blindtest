import { describe, expect, it } from 'vitest'
import type { RoomPlayer } from '../../api/rooms'
import { ranking } from './ranking'

function player(listenerId: number, name: string, score: number): RoomPlayer {
  return { listenerId, name, score, done: false, roundPoints: null }
}

describe('ranking', () => {
  it('sorts by score, best first', () => {
    const ranked = ranking([player(1, 'Raven', 500), player(2, 'Eve', 1750), player(3, 'Lily', 1000)])
    expect(ranked.map((entry) => entry.name)).toEqual(['Eve', 'Lily', 'Raven'])
    expect(ranked.map((entry) => entry.rank)).toEqual([1, 2, 3])
  })

  it('gives the same rank to equal scores', () => {
    const ranked = ranking([player(1, 'Eve', 1000), player(2, 'Lily', 1000), player(3, 'Raven', 0)])
    expect(ranked.map((entry) => entry.rank)).toEqual([1, 1, 3])
  })
})
