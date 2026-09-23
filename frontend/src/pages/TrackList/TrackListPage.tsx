import { useEffect, useRef, useMemo, useState } from 'react'
import { fetchTracks, hasSource, type Track } from '../../api/tracks'
import { AudioPlayer } from '../../components/AudioPlayer/AudioPlayer'
import { TrackTable } from '../../components/TrackTable/TrackTable'
import { filterTracks, type SourceFilter } from './filterTracks'
import { nextTrack, randomTrack } from './playback'
import { sortTracks } from './sortTracks'
import './TrackListPage.css'

export function TrackListPage() {
  const [tracks, setTracks] = useState<Track[] | null>(null)
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [source, setSource] = useState<SourceFilter>('all')
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [shuffle, setShuffle] = useState(false)
  const audioRef = useRef<HTMLAudioElement>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchTracks(controller.signal)
      .then((list) => setTracks(sortTracks(list)))
      .catch(() => {
        // Un rechargement annule la requête en cours (ce n'est pas une erreur)
        if (!controller.signal.aborted) {
          setError(true)
        }
      })
    return () => controller.abort()
  }, [])

  const shown = useMemo(
    () => (tracks ? filterTracks(tracks, query, source) : []),
    [tracks, query, source],
  )
  const withoutSource = useMemo(() => (tracks ?? []).filter((track) => !hasSource(track)).length, [tracks])
  // "Suivant" et le mode aléatoire ne portent que sur les musiques affichées (recherche + filtre
  // en cours) et effectivement jouables maintenant : avoir un lien KHInsider ou YouTube ne suffit
  // pas, il faut que audio_link ait déjà été résolu.
  const playable = useMemo(() => shown.filter((track) => track.audioLink !== null), [shown])

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

  function handlePlay(track: Track) {
    const audio = audioRef.current
    if (!audio || !track.audioLink) return

    if (currentTrack?.id === track.id) {
      if (audio.paused) audio.play()
      else audio.pause()
      return
    }

    playTrack(track)
  }

  // "Suivant" prend la musique suivante dans l'ordre, sauf en mode aléatoire où il en tire une au
  // hasard : c'est le même bouton qui change de comportement, pas deux boutons séparés.
  function handleNext() {
    const track = shuffle
      ? randomTrack(playable, currentTrack?.id)
      : nextTrack(playable, currentTrack?.id)
    if (track) playTrack(track)
  }

  function handleToggleShuffle() {
    const enabling = !shuffle
    setShuffle(enabling)
    // Activer le mode aléatoire alors que rien ne joue encore sert aussi à démarrer l'écoute.
    if (enabling && !currentTrack) {
      const track = randomTrack(playable, undefined)
      if (track) playTrack(track)
    }
  }

  return (
    <>
      <h1>Music Blindtest</h1>
      {error && <p role="alert">Impossible de charger les musiques. Le backend est-il lancé ?</p>}
      {!error && tracks === null && <p>Chargement...</p>}
      {tracks !== null && tracks.length === 0 && (
        <p>Aucune musique en base. Importe le Google Sheet (voir le README).</p>
      )}
      {tracks !== null && tracks.length > 0 && (
        <>
          <div className="controls">
            <input
              type="search"
              placeholder="Rechercher une franchise, un jeu ou une musique"
              aria-label="Rechercher"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
            <select
              aria-label="Filtrer par source"
              value={source}
              onChange={(event) => setSource(event.target.value as SourceFilter)}
            >
              <option value="all">Toutes les sources</option>
              <option value="with">Avec source</option>
              <option value="without">Sans source</option>
            </select>
            <button
              type="button"
              disabled={playable.length === 0}
              aria-pressed={shuffle}
              onClick={handleToggleShuffle}
            >
              🔀 Lecture aléatoire {shuffle && '(activée)'}
            </button>
          </div>
          <p className="summary">
            {shown.length} {shown.length > 1 ? 'musiques affichées' : 'musique affichée'} sur {tracks.length}, dont{' '}
            {withoutSource} sans source audio dans toute la base.
          </p>
          {shown.length > 0 ? (
            <TrackTable
              tracks={shown}
              onPlay={handlePlay}
              playingTrackId={isPlaying ? currentTrack?.id : undefined}
            />
          ) : (
            <p>Aucune musique ne correspond.</p>
          )}
        </>
      )}
      <AudioPlayer ref={audioRef} track={currentTrack} onPlayingChange={setIsPlaying} onNext={handleNext} />
    </>
  )
}
