import { describe, expect, it } from 'vitest'
import type { LeaderboardEntry } from '../../api/blindtests'
import { sortLeaderboard } from './sortLeaderboard'

function entry(listenerName: string, goodAnswers: number, tracksHeard: number): LeaderboardEntry {
  return { listenerName, goodAnswers, bonusAnswers: 0, tracksHeard }
}

describe('sortLeaderboard', () => {
  it('trie par nombre de bonnes réponses par défaut', () => {
    const clement = entry('Clément', 3, 10)
    const nyx = entry('Nyx', 8, 10)

    expect(sortLeaderboard([clement, nyx], 'count')).toEqual([nyx, clement])
  })

  it('trie par pourcentage quand demandé, même si le nombre brut est plus bas', () => {
    // Clément a moins de bonnes réponses en brut, mais un meilleur pourcentage (moins avancé dans la partie)
    const clement = entry('Clément', 3, 4)
    const nyx = entry('Nyx', 8, 20)

    expect(sortLeaderboard([clement, nyx], 'percentage')).toEqual([clement, nyx])
  })

  it("considère un joueur qui n'a encore rien écouté à 0%, pas une erreur", () => {
    const nyx = entry('Nyx', 1, 1)
    const nouveau = entry('Nouveau', 0, 0)

    expect(sortLeaderboard([nouveau, nyx], 'percentage')).toEqual([nyx, nouveau])
  })

  it('ne modifie pas la liste reçue', () => {
    const clement = entry('Clément', 0, 1)
    const nyx = entry('Nyx', 1, 1)
    const original = [nyx, clement]

    sortLeaderboard(original, 'count')

    expect(original).toEqual([nyx, clement])
  })
})
