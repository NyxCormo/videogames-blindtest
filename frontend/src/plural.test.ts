import { describe, expect, it } from 'vitest'
import { plural } from './plural'

describe('plural', () => {
  it('accorde selon le nombre', () => {
    expect(plural(0, 'jeu', 'jeux')).toBe('0 jeu')
    expect(plural(1, 'jeu', 'jeux')).toBe('1 jeu')
    expect(plural(12, 'jeu', 'jeux')).toBe('12 jeux')
  })
})
