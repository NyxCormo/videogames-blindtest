import { useEffect, useState } from 'react'
import { fetchNextTrack, setKnowledge, type DiscoverTrack } from '../../api/discover'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import './DiscoverPage.css'

const SHOW_NAMES_KEY = 'blindtest.discoverShowNames'

function loadShowNames(): boolean {
  try {
    return localStorage.getItem(SHOW_NAMES_KEY) === 'true'
  } catch {
    return false
  }
}

function storeShowNames(value: boolean): void {
  try {
    localStorage.setItem(SHOW_NAMES_KEY, String(value))
  } catch {
    // stockage indisponible : tant pis, le réglage ne sera pas mémorisé
  }
}

export function DiscoverPage() {
  const { listener } = useCurrentListener()
  const [track, setTrack] = useState<DiscoverTrack | null>(null)
  const [error, setError] = useState(false)
  const [showNames, setShowNames] = useState(loadShowNames)
  const [revealed, setRevealed] = useState(false)

  function loadNext() {
    if (!listener) return
    setTrack(null)
    setRevealed(false)
    fetchNextTrack(listener.id)
      .then(setTrack)
      .catch(() => setError(true))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadNext, [listener])

  function toggleShowNames() {
    const next = !showNames
    setShowNames(next)
    storeShowNames(next)
  }

  function answer(knows: boolean) {
    if (!listener || track === null || track.trackId === null) return
    setKnowledge(listener.id, track.trackId, knows).catch(() => {})
    if (showNames) {
      loadNext()
    } else {
      setRevealed(true)
    }
  }

  if (!listener) {
    return <p>Connecte-toi pour découvrir tes musiques.</p>
  }
  if (error) {
    return <p role="alert">Impossible de charger une musique.</p>
  }

  return (
    <>
      <h1>Découvrir</h1>
      <button type="button" aria-pressed={showNames} onClick={toggleShowNames}>
        Afficher les noms {showNames && '(activé)'}
      </button>

      {track === null ? (
        <p>Chargement...</p>
      ) : track.finished ? (
        <p>Tu as déjà voté pour toutes les musiques jouables.</p>
      ) : (
        <>
          <audio key={track.trackId} controls autoPlay src={track.audioLink ?? undefined} />

          {showNames ? (
            <>
              <p className="discover-name">
                <strong>{track.franchiseName}</strong> — {track.gameName} — {track.trackName}
              </p>
              <div className="discover-actions">
                <button type="button" onClick={() => answer(true)}>
                  Oui
                </button>
                <button type="button" onClick={() => answer(false)}>
                  Non
                </button>
              </div>
            </>
          ) : revealed ? (
            <>
              <p className="discover-name">
                <strong>{track.franchiseName}</strong> — {track.gameName} — {track.trackName}
              </p>
              <button type="button" onClick={loadNext}>
                Suivant
              </button>
            </>
          ) : (
            <div className="discover-actions">
              <button type="button" onClick={() => answer(true)}>
                Je connais
              </button>
              <button type="button" onClick={() => answer(false)}>
                Je ne sais pas
              </button>
            </div>
          )}
        </>
      )}
    </>
  )
}
