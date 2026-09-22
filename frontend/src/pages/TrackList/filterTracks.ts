import { hasSource, type Track } from '../../api/tracks'

export type SourceFilter = 'all' | 'with' | 'without'

// Normalzation : "Shaël" -> "shael"
function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

// Chaque mot de la recherche doit se trouver dans la franchise, le jeu ou le titre
export function filterTracks(tracks: Track[], query: string, source: SourceFilter): Track[] {
  const words = normalize(query).split(/\s+/).filter(Boolean)
  return tracks.filter((track) => {
    if (source === 'with' && !hasSource(track)) return false
    if (source === 'without' && hasSource(track)) return false
    const text = normalize(`${track.franchiseName} ${track.gameName} ${track.name}`)
    return words.every((word) => text.includes(word))
  })
}
