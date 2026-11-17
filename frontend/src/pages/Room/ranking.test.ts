import { describe, expect, it } from 'vitest'
import type { RoomPlayer } from '../../api/rooms'
import { rankLabel, ranking, successRate } from './ranking'

function player(listenerId: number, name: string, score: number): RoomPlayer {
  return {
    listenerId,
    name,
    score,
    done: false,
    roundPoints: null,
    present: true,
    gamesFound: 0,
    franchisesFound: 0,
    bonusesFound: 0,
    tracksHeard: 0,
  }
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

describe('rankLabel', () => {
  it('writes the place in French', () => {
    expect([1, 2, 3, 11].map(rankLabel)).toEqual(['1er', '2e', '3e', '11e'])
  })
})

describe('successRate', () => {
  it('compares the games found with the tracks heard', () => {
    expect(successRate(3, 4)).toBe('75 %')
    expect(successRate(1, 3)).toBe('33 %')
    expect(successRate(0, 0)).toBe('–')
  })
})
