import { useEffect, useState, type FormEvent } from 'react'
import { AdminSessionExpired, renameItem, type RenameKind } from '../../api/admin'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { fetchTracks, type Track } from '../../api/tracks'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useNotification } from '../../context/NotificationContext'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function RenameSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const [franchises, setFranchises] = useState<Franchise[]>([])
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [franchiseId, setFranchiseId] = useState<number | null>(null)
  const [gameId, setGameId] = useState<number | null>(null)
  const [trackId, setTrackId] = useState<number | null>(null)

  const franchise = franchises.find((item) => item.id === franchiseId) ?? null
  const franchiseGames = games.filter((item) => item.franchiseId === franchiseId)
  const game = franchiseGames.find((item) => item.id === gameId) ?? null
  const gameTracks = tracks.filter((item) => item.gameId === gameId)
  const track = gameTracks.find((item) => item.id === trackId) ?? null

  function loadAll(signal?: AbortSignal) {
    return Promise.all([fetchFranchises(signal), fetchGames(signal), fetchTracks(signal)]).then(
      ([loadedFranchises, loadedGames, loadedTracks]) => {
        setFranchises(loadedFranchises)
        setGames(loadedGames)
        setTracks(loadedTracks)
      },
    )
  }

  useEffect(() => {
    const controller = new AbortController()
    loadAll(controller.signal).catch(() => {
      if (!controller.signal.aborted) notify('Impossible de charger la base.', 'error')
    })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function chooseFranchise(id: number | null) {
    setFranchiseId(id)
    setGameId(null)
    setTrackId(null)
  }

  function chooseGame(id: number | null) {
    setGameId(id)
    setTrackId(null)
  }

  function rename(kind: RenameKind, id: number, oldName: string, name: string) {
    renameItem(token, kind, id, name)
      .then((updated) => {
        notify(`« ${oldName} » renommé en « ${updated.name} ».`, 'success')
        return loadAll()
      })
      .catch((err: Error) => {
        if (err instanceof AdminSessionExpired) {
          onSessionExpired()
        }
        notify(err.message, 'error')
      })
  }

  return (
    <section className="admin-section">
      <h2>Renommer</h2>
      <div className="admin-cascade">
        <div className="admin-cascade-column">
          <h3>Franchise</h3>
          <div className="admin-cascade-picker">
            <NamePicker
              items={franchises}
              selected={franchise}
              onSelect={(item) => chooseFranchise(item.id)}
              onClear={() => chooseFranchise(null)}
              placeholder="Chercher une franchise"
            />
          </div>
          {franchise && (
            <RenameForm
              key={`${franchise.id}-${franchise.name}`}
              current={franchise.name}
              onRename={(name) => rename('franchises', franchise.id, franchise.name, name)}
            />
          )}
        </div>

        <div className="admin-cascade-column">
          <h3>Jeu</h3>
          <div className="admin-cascade-picker">
            <select
              aria-label="Jeu"
              value={gameId ?? ''}
              onChange={(event) => chooseGame(event.target.value === '' ? null : Number(event.target.value))}
              disabled={franchise === null}
            >
              <option value="">{franchise === null ? "Choisir d'abord une franchise" : 'Choisir un jeu'}</option>
              {franchiseGames.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </div>
          {game && (
            <RenameForm
              key={`${game.id}-${game.name}`}
              current={game.name}
              onRename={(name) => rename('games', game.id, game.name, name)}
            />
          )}
        </div>

        <div className="admin-cascade-column">
          <h3>Musique</h3>
          <div className="admin-cascade-picker">
            <select
              aria-label="Musique"
              value={trackId ?? ''}
              onChange={(event) => setTrackId(event.target.value === '' ? null : Number(event.target.value))}
              disabled={game === null}
            >
              <option value="">{game === null ? "Choisir d'abord un jeu" : 'Choisir une musique'}</option>
              {gameTracks.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </div>
          {track && (
            <RenameForm
              key={`${track.id}-${track.name}`}
              current={track.name}
              onRename={(name) => rename('tracks', track.id, track.name, name)}
            />
          )}
        </div>
      </div>
    </section>
  )
}

type RenameFormProps = {
  current: string
  onRename: (name: string) => void
}

function RenameForm({ current, onRename }: RenameFormProps) {
  const [draft, setDraft] = useState(current)
  const trimmed = draft.trim()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onRename(trimmed)
  }

  return (
    <form className="admin-row" onSubmit={handleSubmit}>
      <input
        type="text"
        aria-label="Nouveau nom"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
      />
      <button type="submit" disabled={trimmed === '' || trimmed === current}>
        Renommer
      </button>
    </form>
  )
}
