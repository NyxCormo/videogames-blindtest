import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { nextTrack, previousTrack, shuffleTracks } from './playback'

function track(id: number, name: string): Track {
  return {
    id,
    name,
    gameId: 1,
    gameName: 'Stellar Blade',
    franchiseName: 'Stellar Blade',
    khinsiderLink: null,
    youtubeLink: null,
    audioLink: `https://example.test/${name}.mp3`,
  }
}

const dawn = track(1, 'Dawn')
const raven = track(2, 'Raven')
const shael = track(3, 'Shaël')
const tracks = [dawn, raven, shael]

describe('nextTrack', () => {
  it('renvoie la musique suivante dans la liste', () => {
    expect(nextTrack(tracks, dawn.id)).toBe(raven)
    expect(nextTrack(tracks, raven.id)).toBe(shael)
  })

  it('revient au début après la dernière', () => {
    expect(nextTrack(tracks, shael.id)).toBe(dawn)
  })

  it('repart du début si rien ne joue', () => {
    expect(nextTrack(tracks, undefined)).toBe(dawn)
  })

  it('repart du début si la musique en cours a disparu de la liste (filtre changé)', () => {
    expect(nextTrack(tracks, 999)).toBe(dawn)
  })

  it('renvoie undefined si la liste est vide', () => {
    expect(nextTrack([], dawn.id)).toBeUndefined()
  })
})

describe('previousTrack', () => {
  it('renvoie la musique précédente dans la liste', () => {
    expect(previousTrack(tracks, shael.id)).toBe(raven)
    expect(previousTrack(tracks, raven.id)).toBe(dawn)
  })

  it('ne renvoie rien avant la première', () => {
    expect(previousTrack(tracks, dawn.id)).toBeUndefined()
  })

  it('ne renvoie rien si la musique en cours est absente de la liste', () => {
    expect(previousTrack(tracks, undefined)).toBeUndefined()
    expect(previousTrack(tracks, 999)).toBeUndefined()
  })
})

describe('shuffleTracks', () => {
  it('mélange selon la fonction random fournie', () => {
    expect(shuffleTracks(tracks, () => 0)).toEqual([raven, shael, dawn])
    expect(shuffleTracks(tracks, () => 0.99)).toEqual([dawn, raven, shael])
  })

  it('garde toutes les musiques, une fois chacune', () => {
    const shuffled = shuffleTracks(tracks)
    expect(shuffled).toHaveLength(3)
    expect(shuffled).toEqual(expect.arrayContaining(tracks))
  })

  it('ne modifie pas la liste reçue', () => {
    shuffleTracks(tracks, () => 0)
    expect(tracks).toEqual([dawn, raven, shael])
  })
})
