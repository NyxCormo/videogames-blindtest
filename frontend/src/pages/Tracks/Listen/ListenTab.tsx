import { useMemo, useState } from 'react'
import { hasSource, type Track } from '../../../api/tracks'
import { PlayIcon, ShuffleIcon } from '../../../components/icons'
import { Select, type SelectOption } from '../../../components/Select/Select'
import { TrackTable } from '../../../components/TrackTable/TrackTable'
import { plural } from '../../../plural'
import type { Player } from '../usePlayer'
import { filterTracks, type SourceFilter } from './filterTracks'

const SOURCE_OPTIONS: SelectOption<SourceFilter>[] = [
  { value: 'all', label: 'Toutes les sources' },
  { value: 'with', label: 'Avec source' },
  { value: 'without', label: 'Sans source' },
]

type Props = {
  // null tant que la liste se charge
  tracks: Track[] | null
  error: boolean
  player: Player
}

export function ListenTab({ tracks, error, player }: Props) {
  const [query, setQuery] = useState('')
  const [source, setSource] = useState<SourceFilter>('all')

  const shown = useMemo(
    () => (tracks ? filterTracks(tracks, query, source) : []),
    [tracks, query, source],
  )
  const withoutSource = useMemo(() => (tracks ?? []).filter((track) => !hasSource(track)).length, [tracks])
  const playable = useMemo(() => shown.filter((track) => track.audioLink !== null), [shown])

  return (
    <>
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
            <Select ariaLabel="Filtrer par source" options={SOURCE_OPTIONS} value={source} onChange={setSource} />
            <button
              type="button"
              disabled={!player.canShuffle}
              aria-pressed={player.shuffle}
              onClick={player.toggleShuffle}
            >
              <ShuffleIcon /> Lecture aléatoire {player.shuffle && '(activée)'}
            </button>
            <button type="button" disabled={playable.length === 0} onClick={() => player.listenTo(playable)}>
              <PlayIcon /> Écouter la sélection
            </button>
          </div>
          <p className="summary">
            {plural(shown.length, 'musique affichée', 'musiques affichées')} sur {tracks.length}, dont{' '}
            {withoutSource} sans source audio dans toute la base.
          </p>
          <p className="summary">
            File de lecture : {plural(player.queueLength, 'musique', 'musiques')}
            {player.hasSelection ? ' (sélection)' : ' (toute la base)'}
            {player.shuffle && ', ordre aléatoire'}.
          </p>
          {shown.length > 0 ? (
            <TrackTable
              tracks={shown}
              onPlay={player.play}
              playingTrackId={player.isPlaying ? player.currentTrack?.id : undefined}
              resetKey={`${query}|${source}`}
            />
          ) : (
            <p>Aucune musique ne correspond.</p>
          )}
        </>
      )}
    </>
  )
}
