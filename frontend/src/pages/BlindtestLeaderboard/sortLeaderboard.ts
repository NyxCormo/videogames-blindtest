import type { LeaderboardEntry } from '../../api/blindtests'

export type LeaderboardSort = 'count' | 'percentage'

function percentage(entry: LeaderboardEntry): number {
  return entry.tracksHeard > 0 ? entry.goodAnswers / entry.tracksHeard : 0
}

export function sortLeaderboard(entries: LeaderboardEntry[], sort: LeaderboardSort): LeaderboardEntry[] {
  if (sort === 'percentage') {
    return [...entries].sort((a, b) => percentage(b) - percentage(a))
  }
  return [...entries].sort((a, b) => b.goodAnswers - a.goodAnswers)
}
