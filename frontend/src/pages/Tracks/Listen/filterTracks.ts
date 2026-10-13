import { hasSource, type Track } from '../../../api/tracks'
import { matchesWords, searchWords } from '../../../textSearch'

export type SourceFilter = 'all' | 'with' | 'without'

// Chaque mot de la recherche doit se trouver dans la franchise, le jeu ou le titre
export function filterTracks(tracks: Track[], query: string, source: SourceFilter): Track[] {
  const words = searchWords(query)
  return tracks.filter((track) => {
    if (source === 'with' && !hasSource(track)) return false
    if (source === 'without' && hasSource(track)) return false
    return matchesWords(`${track.franchiseName} ${track.gameName} ${track.name}`, words)
  })
}
