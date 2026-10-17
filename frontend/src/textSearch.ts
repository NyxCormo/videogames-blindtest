const LIGATURES: Record<string, string> = { œ: 'oe', æ: 'ae', ß: 'ss' }

// "Pokémon : Rouge" -> "pokemonrouge" : seules les lettres et les chiffres comptent.
export function normalize(text: string): string {
  return text
    .toLowerCase()
    .replace(/[œæß]/g, (letter) => LIGATURES[letter])
    .normalize('NFD')
    .replace(/[^\p{L}\p{N}]/gu, '')
}

export function searchWords(query: string): string[] {
  return query.split(/\s+/).map(normalize).filter(Boolean)
}

// Chaque mot de la recherche doit se trouver dans le texte, dans n'importe quel ordre.
export function matchesWords(text: string, words: string[]): boolean {
  const normalized = normalize(text)
  return words.every((word) => normalized.includes(word))
}
