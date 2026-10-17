import { describe, expect, it } from 'vitest'
import { forget, remember, rename } from './recentListeners'

const nyx = { id: 1, name: 'Nyx' }
const awing = { id: 2, name: 'Awing' }

describe('pseudos récents', () => {
  it('met le dernier pseudo utilisé en tête, sans doublon', () => {
    expect(remember([nyx, awing], awing)).toEqual([awing, nyx])
    expect(remember([], nyx)).toEqual([nyx])
  })

  it('en garde cinq au plus', () => {
    const many = [1, 2, 3, 4, 5].map((id) => ({ id, name: `Joueur ${id}` }))
    expect(remember(many, { id: 6, name: 'Joueur 6' }).map((listener) => listener.id)).toEqual([6, 1, 2, 3, 4])
  })

  it('oublie un pseudo qui n’existe plus et suit un renommage', () => {
    expect(forget([nyx, awing], 1)).toEqual([awing])
    expect(rename([nyx, awing], { id: 1, name: 'NyxCormo' })).toEqual([{ id: 1, name: 'NyxCormo' }, awing])
  })
})
