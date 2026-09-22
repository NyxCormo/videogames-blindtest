import { useEffect, useMemo, useState } from 'react'
import { fetchTracks, hasSource, type Track } from '../../api/tracks'
import { TrackTable } from '../../components/TrackTable/TrackTable'
import { filterTracks, type SourceFilter } from './filterTracks'
import { sortTracks } from './sortTracks'
import './TrackListPage.css'

export function TrackListPage() {
  const [tracks, setTracks] = useState<Track[] | null>(null)
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [source, setSource] = useState<SourceFilter>('all')

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
          </div>
          <p className="summary">
            {shown.length} {shown.length > 1 ? 'musiques affichées' : 'musique affichée'} sur {tracks.length}, dont{' '}
            {withoutSource} sans source audio dans toute la base.
          </p>
          {shown.length > 0 ? <TrackTable tracks={shown} /> : <p>Aucune musique ne correspond.</p>}
        </>
      )}
    </>
  )
}
