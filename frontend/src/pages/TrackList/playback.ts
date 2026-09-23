import type { Track } from '../../api/tracks'

// La musique suivante dans la liste (celle qui vient après currentTrackId). Si currentTrackId
// n'est pas dans la liste (rien ne joue encore, ou le filtre a changé), on repart du début.
export function nextTrack(tracks: Track[], currentTrackId?: number): Track | undefined {
  if (tracks.length === 0) return undefined
  const index = tracks.findIndex((track) => track.id === currentTrackId)
  return tracks[(index + 1) % tracks.length]
}

// Une musique au hasard, différente de celle en cours quand c'est possible (pas de répétition
// immédiate s'il y a le choix). `random` est injectable pour pouvoir tester sans dépendre du hasard.
export function randomTrack(
  tracks: Track[],
  currentTrackId?: number,
  random: () => number = Math.random,
): Track | undefined {
  if (tracks.length === 0) return undefined
  const candidates = tracks.length > 1 ? tracks.filter((track) => track.id !== currentTrackId) : tracks
  return candidates[Math.floor(random() * candidates.length)]
}
