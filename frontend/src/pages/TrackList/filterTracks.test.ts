import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { filterTracks } from './filterTracks'

const dawn: Track = {
  id: 1,
  name: 'Dawn',
  gameName: 'Stellar Blade',
  franchiseName: 'Stellar Blade',
  khinsiderLink: 'https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024/62.%2520Dawn.mp3',
  youtubeLink: null,
  audioLink: null
}
const raven: Track = { ...dawn, id: 2, name: 'Raven', khinsiderLink: null }
const shael: Track = { ...dawn, id: 3, name: 'Shaël' }
const tracks = [dawn, raven, shael]

describe('filterTracks', () => {
  it('garde tout sans recherche ni filtre', () => {
    expect(filterTracks(tracks, '', 'all')).toEqual(tracks)
    expect(filterTracks(tracks, '   ', 'all')).toEqual(tracks)
  })

  it('ignore la casse et les accents', () => {
    expect(filterTracks(tracks, 'SHAEL', 'all')).toEqual([shael])
    expect(filterTracks(tracks, 'shaël', 'all')).toEqual([shael])
  })

  it('exige tous les mots de la recherche', () => {
    expect(filterTracks(tracks, 'stellar raven', 'all')).toEqual([raven])
    expect(filterTracks(tracks, 'stellar zelda', 'all')).toEqual([])
  })

  it('filtre selon la présence d’une source', () => {
    expect(filterTracks(tracks, '', 'with')).toEqual([dawn, shael])
    expect(filterTracks(tracks, '', 'without')).toEqual([raven])
  })

  it('combine recherche et source', () => {
    expect(filterTracks(tracks, 'raven', 'with')).toEqual([])
    expect(filterTracks(tracks, 'raven', 'without')).toEqual([raven])
  })

  it('compte une musique YouTube seule comme ayant une source', () => {
    const onlyYoutube: Track = { ...raven, id: 4, youtubeLink: 'https://www.youtube.com/watch?v=exemple' }
    expect(filterTracks([onlyYoutube], '', 'with')).toEqual([onlyYoutube])
  })
})
