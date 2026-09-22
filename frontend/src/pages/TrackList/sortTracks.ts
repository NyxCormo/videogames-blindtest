import type { Track } from '../../api/tracks'

// Tri avec casse et acents ignorés (SQLite ne sait pas faire cela)
const collator = new Intl.Collator('fr', { sensitivity: 'base' })

export function sortTracks(tracks: Track[]): Track[] {
  return [...tracks].sort(
    (a, b) =>
      collator.compare(a.franchiseName, b.franchiseName) ||
      collator.compare(a.gameName, b.gameName) ||
      collator.compare(a.name, b.name),
  )
}
