import { describe, expect, it } from 'vitest'
import { thumbnailOf } from './coverThumbnail'

describe('miniature d’une pochette', () => {
  it('prend le dossier thumbs de l’album sur le CDN', () => {
    expect(thumbnailOf('https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/00%20Front.jpg')).toBe(
      'https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/thumbs/00%20Front.jpg',
    )
    expect(thumbnailOf('https://vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/folder.jpg')).toBe(
      'https://vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/thumbs/folder.jpg',
    )
  })

  it('garde les autres adresses telles quelles', () => {
    expect(thumbnailOf('https://example.org/stellar-blade.png')).toBe('https://example.org/stellar-blade.png')
    expect(thumbnailOf('/api/games/63/cover?v=3713780b')).toBe('/api/games/63/cover?v=3713780b')
  })
})
