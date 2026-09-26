import { describe, expect, it } from 'vitest'
import type { LeaderboardEntry } from '../../api/blindtests'
import { rankLeaderboard, sortLeaderboard } from './sortLeaderboard'

function entry(
  listenerName: string,
  goodAnswers: number,
  franchiseAnswers: number,
  bonusAnswers: number,
  tracksHeard: number,
): LeaderboardEntry {
  return { listenerName, goodAnswers, franchiseAnswers, bonusAnswers, tracksHeard }
}

describe('sortLeaderboard', () => {
  it('trie par nombre de bonnes réponses, décroissant', () => {
    const clement = entry('Clément', 3, 0, 0, 10)
    const nyx = entry('Nyx', 8, 0, 0, 10)

    expect(sortLeaderboard([clement, nyx], 'good', 'desc')).toEqual([nyx, clement])
  })

  it('inverse l’ordre en ascendant', () => {
    const clement = entry('Clément', 3, 0, 0, 10)
    const nyx = entry('Nyx', 8, 0, 0, 10)

    expect(sortLeaderboard([clement, nyx], 'good', 'asc')).toEqual([clement, nyx])
  })

  it('trie par pourcentage de bonnes réponses, même si le nombre brut est plus bas', () => {
    // Clément a moins de bonnes réponses en brut, mais un meilleur pourcentage (moins avancé dans la partie)
    const clement = entry('Clément', 3, 0, 0, 4)
    const nyx = entry('Nyx', 8, 0, 0, 20)

    expect(sortLeaderboard([clement, nyx], 'goodPercent', 'desc')).toEqual([clement, nyx])
  })

  it("considère un joueur qui n'a encore rien écouté à 0%, pas une erreur", () => {
    const nyx = entry('Nyx', 1, 0, 0, 1)
    const nouveau = entry('Nouveau', 0, 0, 0, 0)

    expect(sortLeaderboard([nouveau, nyx], 'goodPercent', 'desc')).toEqual([nyx, nouveau])
  })

  it('ne modifie pas la liste reçue', () => {
    const clement = entry('Clément', 0, 0, 0, 1)
    const nyx = entry('Nyx', 1, 0, 0, 1)
    const original = [nyx, clement]

    sortLeaderboard(original, 'good', 'desc')

    expect(original).toEqual([nyx, clement])
  })

  it('trie par franchises trouvées, indépendamment des bonnes réponses', () => {
    const clement = entry('Clément', 8, 1, 0, 10)
    const nyx = entry('Nyx', 3, 5, 0, 10)

    expect(sortLeaderboard([clement, nyx], 'franchise', 'desc')).toEqual([nyx, clement])
  })

  it('trie par pourcentage de franchises trouvées', () => {
    const clement = entry('Clément', 0, 4, 0, 4)
    const nyx = entry('Nyx', 0, 8, 0, 20)

    expect(sortLeaderboard([clement, nyx], 'franchisePercent', 'desc')).toEqual([clement, nyx])
  })

  it('trie par musiques bonus, indépendamment des bonnes réponses', () => {
    // Clément a plus de bonnes réponses, mais moins de bonus
    const clement = entry('Clément', 8, 0, 1, 10)
    const nyx = entry('Nyx', 3, 0, 5, 10)

    expect(sortLeaderboard([clement, nyx], 'bonus', 'desc')).toEqual([nyx, clement])
  })

  it('trie par pourcentage de musiques bonus', () => {
    const clement = entry('Clément', 4, 0, 1, 4)
    const nyx = entry('Nyx', 8, 0, 4, 20)

    expect(sortLeaderboard([clement, nyx], 'bonusPercent', 'desc')).toEqual([clement, nyx])
  })

  it('trie par nombre de musiques écoutées', () => {
    const clement = entry('Clément', 0, 0, 0, 3)
    const nyx = entry('Nyx', 0, 0, 0, 8)

    expect(sortLeaderboard([clement, nyx], 'tracksHeard', 'desc')).toEqual([nyx, clement])
  })

  it('trie par nom, alphabétiquement', () => {
    const nyx = entry('Nyx', 0, 0, 0, 0)
    const clement = entry('Clément', 0, 0, 0, 0)

    expect(sortLeaderboard([nyx, clement], 'name', 'asc')).toEqual([clement, nyx])
  })
})

describe('rankLeaderboard', () => {
  it('classe par bonnes réponses en premier', () => {
    const clement = entry('Clément', 3, 0, 0, 10)
    const nyx = entry('Nyx', 8, 0, 0, 10)

    expect(rankLeaderboard([clement, nyx])).toEqual([nyx, clement])
  })

  it('départage une égalité de bonnes réponses par leur pourcentage', () => {
    // même nombre de bonnes réponses, mais Clément a moins écouté : meilleur pourcentage
    const clement = entry('Clément', 3, 0, 0, 4)
    const nyx = entry('Nyx', 3, 0, 0, 10)

    expect(rankLeaderboard([nyx, clement])).toEqual([clement, nyx])
  })

  it('départage une égalité de bonnes réponses et de pourcentage par les franchises trouvées', () => {
    const clement = entry('Clément', 3, 2, 0, 10)
    const nyx = entry('Nyx', 3, 1, 0, 10)

    expect(rankLeaderboard([nyx, clement])).toEqual([clement, nyx])
  })

  it('départage une égalité de bonnes réponses, pourcentage et franchises par les musiques bonus', () => {
    const clement = entry('Clément', 3, 1, 2, 10)
    const nyx = entry('Nyx', 3, 1, 1, 10)

    expect(rankLeaderboard([nyx, clement])).toEqual([clement, nyx])
  })

  it('départage une égalité totale par ordre alphabétique', () => {
    const nyx = entry('Nyx', 3, 1, 1, 10)
    const clement = entry('Clément', 3, 1, 1, 10)

    expect(rankLeaderboard([nyx, clement])).toEqual([clement, nyx])
  })

  it('ne modifie pas la liste reçue', () => {
    const clement = entry('Clément', 0, 0, 0, 1)
    const nyx = entry('Nyx', 1, 0, 0, 1)
    const original = [clement, nyx]

    rankLeaderboard(original)

    expect(original).toEqual([clement, nyx])
  })
})
