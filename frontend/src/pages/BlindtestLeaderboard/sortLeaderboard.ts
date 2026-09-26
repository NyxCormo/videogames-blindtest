import type { LeaderboardEntry } from '../../api/blindtests'

export type LeaderboardColumn =
  | 'name'
  | 'good'
  | 'goodPercent'
  | 'franchise'
  | 'franchisePercent'
  | 'bonus'
  | 'bonusPercent'
  | 'tracksHeard'
export type SortDirection = 'asc' | 'desc'

// Tri avec casse et accents ignorés (SQLite ne sait pas faire cela), même principe que sortTracks
const collator = new Intl.Collator('fr', { sensitivity: 'base' })

function percent(count: number, tracksHeard: number): number {
  return tracksHeard > 0 ? count / tracksHeard : 0
}

function columnValue(entry: LeaderboardEntry, column: LeaderboardColumn): number | string {
  switch (column) {
    case 'name':
      return entry.listenerName
    case 'good':
      return entry.goodAnswers
    case 'goodPercent':
      return percent(entry.goodAnswers, entry.tracksHeard)
    case 'franchise':
      return entry.franchiseAnswers
    case 'franchisePercent':
      return percent(entry.franchiseAnswers, entry.tracksHeard)
    case 'bonus':
      return entry.bonusAnswers
    case 'bonusPercent':
      return percent(entry.bonusAnswers, entry.tracksHeard)
    case 'tracksHeard':
      return entry.tracksHeard
  }
}

export function sortLeaderboard(
  entries: LeaderboardEntry[],
  column: LeaderboardColumn,
  direction: SortDirection,
): LeaderboardEntry[] {
  const sorted = [...entries].sort((a, b) => {
    const valueA = columnValue(a, column)
    const valueB = columnValue(b, column)
    return typeof valueA === 'string' && typeof valueB === 'string'
      ? collator.compare(valueA, valueB)
      : (valueA as number) - (valueB as number)
  })
  return direction === 'asc' ? sorted : sorted.reverse()
}

// Classement officiel (colonne "Rang") : bonnes réponses, puis leur pourcentage, puis la franchise (elle
// compte pour le score, contrairement au bonus), puis les musiques bonus, puis ordre alphabétique. Les
// pourcentages de franchise et de bonus n'entrent pas dans le départage : à bonnes réponses, pourcentage
// et franchise (ou bonus) égaux, les musiques écoutées le sont aussi (pourcentage = compte / écoutées),
// donc il ne peut plus rien départager de plus.
function rankCompare(a: LeaderboardEntry, b: LeaderboardEntry): number {
  return (
    b.goodAnswers - a.goodAnswers ||
    percent(b.goodAnswers, b.tracksHeard) - percent(a.goodAnswers, a.tracksHeard) ||
    b.franchiseAnswers - a.franchiseAnswers ||
    b.bonusAnswers - a.bonusAnswers ||
    collator.compare(a.listenerName, b.listenerName)
  )
}

export function rankLeaderboard(entries: LeaderboardEntry[]): LeaderboardEntry[] {
  return [...entries].sort(rankCompare)
}
