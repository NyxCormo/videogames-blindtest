import { describe, expect, it } from 'vitest'
import type { Track } from '../../api/tracks'
import { nextTrack, randomTrack } from './playback'

function track(id: number, name: string): Track {
  return {
    id,
    name,
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

describe('randomTrack', () => {
  it('choisit selon la fonction random fournie', () => {
    expect(randomTrack(tracks, undefined, () => 0)).toBe(dawn)
    expect(randomTrack(tracks, undefined, () => 0.99)).toBe(shael)
  })

  it('ne rechoisit jamais la même musique quand il y a le choix', () => {
    // random() = 0 choisirait normalement la première candidate ; ça ne doit jamais être dawn ici
    expect(randomTrack(tracks, dawn.id, () => 0)).toBe(raven)
  })

  it('peut renvoyer la même musique si elle est seule dans la liste', () => {
    expect(randomTrack([dawn], dawn.id, () => 0)).toBe(dawn)
  })

  it('renvoie undefined si la liste est vide', () => {
    expect(randomTrack([], undefined)).toBeUndefined()
  })
})
