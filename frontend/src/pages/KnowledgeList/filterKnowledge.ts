import type { Track } from '../../api/tracks'

export type VoteFilter = 'all' | 'yes' | 'no' | 'none'

// Normalzation : "Shaël" -> "shael"
function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

// Chaque mot de la recherche doit se trouver dans la franchise, le jeu ou le titre
export function filterKnowledge(
  tracks: Track[],
  query: string,
  vote: VoteFilter,
  knowledge: Map<number, boolean>,
): Track[] {
  const words = normalize(query).split(/\s+/).filter(Boolean)
  return tracks.filter((track) => {
    const knows = knowledge.get(track.id)
    if (vote === 'yes' && knows !== true) return false
    if (vote === 'no' && knows !== false) return false
    if (vote === 'none' && knows !== undefined) return false
    const text = normalize(`${track.franchiseName} ${track.gameName} ${track.name}`)
    return words.every((word) => text.includes(word))
  })
}
