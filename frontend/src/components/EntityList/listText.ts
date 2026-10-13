import { matchesWords, searchWords } from '../../textSearch'

export function plural(count: number, singular: string, pluralForm: string): string {
  return `${count} ${count > 1 ? pluralForm : singular}`
}

export function sortByText<T>(items: T[], getText: (item: T) => string): T[] {
  return [...items].sort((a, b) => getText(a).localeCompare(getText(b), 'fr', { sensitivity: 'base' }))
}

export function filterByText<T>(items: T[], query: string, getText: (item: T) => string): T[] {
  const words = searchWords(query)
  if (words.length === 0) return items
  return items.filter((item) => matchesWords(getText(item), words))
}
