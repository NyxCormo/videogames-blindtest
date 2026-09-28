import { describe, expect, it } from 'vitest'
import type { Franchise } from '../../api/franchises'
import type { Game } from '../../api/games'
import type { Track } from '../../api/tracks'
import { filterEnrichment } from './filterEnrichment'

const franchiseA: Franchise = { id: 1, name: 'Stellar Blade' }
const franchiseB: Franchise = { id: 2, name: 'Zelda' }
const franchiseC: Franchise = { id: 3, name: 'Sans jeu' }
const franchises = [franchiseA, franchiseB, franchiseC]

const gameNoTrack: Game = { id: 1, name: 'Jeu sans musique', franchiseId: 1, franchiseName: 'Stellar Blade' }
const gameTwoTracks: Game = { id: 2, name: 'Stellar Blade', franchiseId: 1, franchiseName: 'Stellar Blade' }
const gameOneTrack: Game = { id: 3, name: 'Wind Waker', franchiseId: 2, franchiseName: 'Zelda' }
const games = [gameNoTrack, gameTwoTracks, gameOneTrack]

const dawn: Track = {
  id: 1,
  name: 'Dawn',
  gameId: 2,
  gameName: 'Stellar Blade',
  franchiseName: 'Stellar Blade',
  khinsiderLink: null,
  youtubeLink: null,
  audioLink: null,
}
const raven: Track = { ...dawn, id: 2, name: 'Raven', khinsiderLink: 'https://downloads.khinsider.com/raven' }
const molgera: Track = { ...dawn, id: 3, name: 'Molgera', gameId: 3, gameName: 'Wind Waker' }
const tracks = [dawn, raven, molgera]

function filter(
  query = '',
  onlyFranchisesWithoutGames = false,
  onlyGamesWithoutTracks = false,
  maxTracksPerGame = 25,
  onlyTracksWithoutLinks = false,
) {
  return filterEnrichment(
    franchises,
    games,
    tracks,
    query,
    onlyFranchisesWithoutGames,
    onlyGamesWithoutTracks,
    maxTracksPerGame,
    onlyTracksWithoutLinks,
  )
}

describe('filterEnrichment', () => {
  it('garde tout sans filtre, y compris les franchises et jeux sans musique', () => {
    const result = filter()
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Stellar Blade', 'Zelda', 'Sans jeu'])
    expect(result[0].games).toEqual([gameNoTrack, gameTwoTracks])
    expect(result[2].games).toEqual([])
  })

  it('ignore la casse et les accents dans la recherche', () => {
    const result = filter('ZELDA')
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Zelda'])
  })

  it('ne montre que les franchises sans jeu quand la case est cochée', () => {
    const result = filter('', true)
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Sans jeu'])
  })

  it('ne montre que les jeux sans musique quand la case est cochée, sans cacher les franchises sans jeu', () => {
    const result = filter('', false, true)
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Stellar Blade', 'Sans jeu'])
    expect(result[0].games).toEqual([gameNoTrack])
  })

  it('limite aux jeux ayant au plus N musiques', () => {
    const result = filter('', false, false, 1)
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Stellar Blade', 'Zelda', 'Sans jeu'])
    expect(result[0].games).toEqual([gameNoTrack])
    expect(result[1].games).toEqual([gameOneTrack])
  })

  it('ne montre que les jeux ayant au moins une musique sans lien, sans cacher les franchises sans jeu', () => {
    const result = filter('', false, false, 25, true)
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Stellar Blade', 'Zelda', 'Sans jeu'])
    expect(result[0].games).toEqual([gameTwoTracks])
    expect(result[1].games).toEqual([gameOneTrack])
  })

  it('ne cache pas les jeux sans musique quand "jeux sans musique" et "musiques sans lien" sont cochées ensemble', () => {
    const result = filter('', false, true, 25, true)
    expect(result.map((entry) => entry.franchise.name)).toEqual(['Stellar Blade', 'Sans jeu'])
    expect(result[0].games).toEqual([gameNoTrack])
  })
})
