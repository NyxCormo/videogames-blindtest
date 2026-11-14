import { describe, expect, it } from 'vitest'
import type { Room } from '../../api/rooms'
import { trackProgress } from './roomLabels'

describe('trackProgress', () => {
  it('shows the total only for a blindtest room', () => {
    const room = { infinite: false, trackCount: 10 } as Room
    expect(trackProgress(room, 2)).toBe('Musique 3 sur 10')
    expect(trackProgress({ ...room, infinite: true, trackCount: 0 }, 11)).toBe('Musique 12')
  })
})
