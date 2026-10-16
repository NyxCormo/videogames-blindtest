import { describe, expect, it } from 'vitest'
import { filterByText, sortByText } from './listText'

describe('sortByText', () => {
  it('trie sans tenir compte des majuscules ni des accents, sans modifier la liste reçue', () => {
    const items = ['Zelda', 'A dance of fire and ice', 'A Plague Tale', 'Écho']
    expect(sortByText(items, (item) => item)).toEqual(['A dance of fire and ice', 'A Plague Tale', 'Écho', 'Zelda'])
    expect(items[0]).toBe('Zelda')
  })
})

describe('filterByText', () => {
  const items = ['Stellar Blade', 'Tekken', 'Zelda']

  it('garde les éléments qui contiennent le texte, sans tenir compte de la casse', () => {
    expect(filterByText(items, 'STELLAR', (item) => item)).toEqual(['Stellar Blade'])
  })

  it('renvoie tout quand le filtre est vide ou fait d’espaces', () => {
    expect(filterByText(items, '  ', (item) => item)).toEqual(items)
  })
})
