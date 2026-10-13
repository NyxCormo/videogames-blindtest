import type { Track } from '../../../api/tracks'
import { matchesWords, searchWords } from '../../../textSearch'

export type VoteFilter = 'all' | 'yes' | 'no' | 'none'

// Chaque mot de la recherche doit se trouver dans la franchise, le jeu ou le titre
export function filterKnowledge(
  tracks: Track[],
  query: string,
  vote: VoteFilter,
  knowledge: Map<number, boolean>,
): Track[] {
  const words = searchWords(query)
  return tracks.filter((track) => {
    const knows = knowledge.get(track.id)
    if (vote === 'yes' && knows !== true) return false
    if (vote === 'no' && knows !== false) return false
    if (vote === 'none' && knows !== undefined) return false
    return matchesWords(`${track.franchiseName} ${track.gameName} ${track.name}`, words)
  })
}
