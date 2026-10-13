import { describe, expect, it } from 'vitest'
import { matchesWords, searchWords } from './textSearch'

function matches(text: string, query: string): boolean {
  return matchesWords(text, searchWords(query))
}

describe('searchWords', () => {
  it('découpe la recherche en mots sans accents ni ponctuation', () => {
    expect(searchWords('  Shaël,  DAWN ')).toEqual(['shael', 'dawn'])
  })

  it('ne garde aucun mot quand la recherche ne contient ni lettre ni chiffre', () => {
    expect(searchWords(' - . ')).toEqual([])
  })
})

describe('matchesWords', () => {
  it('ignore les accents et la casse', () => {
    expect(matches('Shaël', 'shael')).toBe(true)
    expect(matches('Shael', 'SHAËL')).toBe(true)
  })

  it('ignore la ponctuation et les espaces du texte', () => {
    expect(matches('Stellar Blade: Dawn', 'stellar blade dawn')).toBe(true)
    expect(matches('Stellar-Blade', 'stellarblade')).toBe(true)
    expect(matches('Stellar Blade', 'stellar-blade')).toBe(true)
  })

  it('accepte les mots dans n’importe quel ordre', () => {
    expect(matches('Stellar Blade Dawn', 'dawn stellar')).toBe(true)
  })

  it('demande que tous les mots soient présents', () => {
    expect(matches('Stellar Blade Dawn', 'dawn raven')).toBe(false)
  })

  it('garde les chiffres', () => {
    expect(matches('Democrawler 2', 'democrawler 2')).toBe(true)
    expect(matches('Democrawler 2', 'democrawler 3')).toBe(false)
  })

  it('accepte tout quand il n’y a aucun mot', () => {
    expect(matchesWords('Raven', [])).toBe(true)
  })
})
