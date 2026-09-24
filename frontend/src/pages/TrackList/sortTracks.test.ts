import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { sortTracks } from './sortTracks'

function track(id: number, franchiseName: string, gameName: string, name: string): Track {
  return { id, name, gameId: 1, gameName, franchiseName, khinsiderLink: null, youtubeLink: null, audioLink: null }
}

describe('sortTracks', () => {
  it('ignore la casse : une minuscule ne passe pas après toutes les majuscules', () => {
    const democrawler = track(1, 'Stellar Blade', 'Stellar Blade', 'Democrawler')
    const raven = track(2, 'Stellar Blade', 'Stellar Blade', 'raven')
    const shael = track(3, 'Stellar Blade', 'Stellar Blade', 'Shaël')

    expect(sortTracks([shael, raven, democrawler])).toEqual([democrawler, raven, shael])
  })

  it('trie par franchise, puis par jeu, puis par musique', () => {
    const dawn = track(1, 'Stellar Blade', 'Stellar Blade', 'Dawn')
    const raven = track(2, 'Stellar Blade', 'Stellar Blade', 'Raven')
    const other = track(3, 'Stellar Blade', 'Stellar Blade: Blood Rain', 'Dawn')
    const later = track(4, 'Stellar Blade (Licence)', 'Stellar Blade', 'Dawn')

    expect(sortTracks([later, other, raven, dawn])).toEqual([dawn, raven, other, later])
  })

  it('ne modifie pas la liste reçue', () => {
    const raven = track(1, 'Stellar Blade', 'Stellar Blade', 'Raven')
    const dawn = track(2, 'Stellar Blade', 'Stellar Blade', 'Dawn')
    const original = [raven, dawn]

    sortTracks(original)

    expect(original).toEqual([raven, dawn])
  })
})
