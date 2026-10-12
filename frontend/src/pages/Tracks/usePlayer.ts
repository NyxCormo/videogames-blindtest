import { useMemo, useState, type RefObject } from 'react'
import type { Track } from '../../api/tracks'
import { nextTrack, previousTrack, shuffleTracks } from './Listen/playback'

function startingWith(track: Track, tracks: Track[]): Track[] {
  const others = tracks.filter((other) => other.id !== track.id)
  return others.length < tracks.length ? [track, ...others] : tracks
}

export type Player = ReturnType<typeof usePlayer>

export function usePlayer(tracks: Track[] | null, audioRef: RefObject<HTMLAudioElement | null>) {
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [selection, setSelection] = useState<Track[] | null>(null)
  const [shuffled, setShuffled] = useState<Track[] | null>(null)

  // Jouable maintenant : avoir un lien KHInsider ou YouTube ne suffit pas, il faut que audio_link
  // ait déjà été résolu.
  const allPlayable = useMemo(() => (tracks ?? []).filter((track) => track.audioLink !== null), [tracks])
  const queueSource = selection ?? allPlayable
  const queue = shuffled ?? queueSource
  const shuffle = shuffled !== null

  // Le .play() est appelé directement dans un clic : c'est ce qui compte comme une interaction
  // utilisateur pour le navigateur (une mise à jour de state React, elle, serait trop tardive).
  function playTrack(track: Track) {
    const audio = audioRef.current
    if (!audio || !track.audioLink) return
    audio.src = track.audioLink
    // Un clic rapide sur "suivant" pendant qu'une lecture précédente démarre encore annule
    // cette dernière : c'est un AbortError normal, pas une vraie erreur à remonter.
    audio.play().catch(() => {})
    setCurrentTrack(track)
  }

  function play(track: Track) {
    const audio = audioRef.current
    if (!audio || !track.audioLink) return

    if (currentTrack?.id === track.id) {
      if (audio.paused) audio.play()
      else audio.pause()
      return
    }

    if (!queue.some((queued) => queued.id === track.id)) {
      setSelection(null)
      setShuffled(shuffle ? startingWith(track, shuffleTracks(allPlayable)) : null)
    }
    playTrack(track)
  }

  function next() {
    const track = nextTrack(queue, currentTrack?.id)
    if (track) playTrack(track)
  }

  function previous() {
    const track = previousTrack(queue, currentTrack?.id)
    if (track) playTrack(track)
  }

  function toggleShuffle() {
    if (shuffle) {
      setShuffled(null)
      return
    }
    const mixed = shuffleTracks(queueSource)
    if (currentTrack) {
      setShuffled(startingWith(currentTrack, mixed))
    } else {
      setShuffled(mixed)
      // Activer le mode aléatoire alors que rien ne joue encore sert aussi à démarrer l'écoute.
      if (mixed.length > 0) playTrack(mixed[0])
    }
  }

  function listenTo(playable: Track[]) {
    const mixed = shuffle ? shuffleTracks(playable) : null
    setSelection(playable)
    setShuffled(mixed)
    const first = (mixed ?? playable)[0]
    if (first) playTrack(first)
  }

  return {
    currentTrack,
    isPlaying,
    setIsPlaying,
    shuffle,
    queueLength: queue.length,
    canShuffle: queueSource.length > 0,
    hasSelection: selection !== null,
    canGoBack: previousTrack(queue, currentTrack?.id) !== undefined,
    play,
    next,
    previous,
    toggleShuffle,
    listenTo,
  }
}
