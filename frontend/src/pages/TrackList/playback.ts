import type { Track } from '../../api/tracks'

// La musique suivante dans la liste (celle qui vient après currentTrackId). Si currentTrackId
// n'est pas dans la liste (rien ne joue encore, ou le filtre a changé), on repart du début.
export function nextTrack(tracks: Track[], currentTrackId?: number): Track | undefined {
  if (tracks.length === 0) return undefined
  const index = tracks.findIndex((track) => track.id === currentTrackId)
  return tracks[(index + 1) % tracks.length]
}

export function previousTrack(tracks: Track[], currentTrackId?: number): Track | undefined {
  const index = tracks.findIndex((track) => track.id === currentTrackId)
  return index > 0 ? tracks[index - 1] : undefined
}

export function shuffleTracks(tracks: Track[], random: () => number = Math.random): Track[] {
  const shuffled = [...tracks]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1))
    const swapped = shuffled[i]
    shuffled[i] = shuffled[j]
    shuffled[j] = swapped
  }
  return shuffled
}
