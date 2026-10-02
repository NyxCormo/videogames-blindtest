import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { AdminSessionExpired, mergeItem, renameItem, type RenameKind } from '../../api/admin'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { fetchTracks, type Track } from '../../api/tracks'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useNotification } from '../../context/NotificationContext'

type Props = {
  token: string
  onSessionExpired: () => void
}

type Named = { id: number; name: string }

export function EditSection({ token, onSessionExpired }: Props) {
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

  function handleError(err: Error) {
    if (err instanceof AdminSessionExpired) {
      onSessionExpired()
    }
    notify(err.message, 'error')
  }

  function rename(kind: RenameKind, id: number, oldName: string, name: string) {
    renameItem(token, kind, id, name)
      .then((updated) => {
        notify(`« ${oldName} » renommé en « ${updated.name} ».`, 'success')
        return loadAll()
      })
      .catch(handleError)
  }

  // Après une fusion, la cascade affiche l'élément gardé.
  function merge(kind: RenameKind, source: Named, target: Named, confirmation: string, onDone: () => void) {
    if (!window.confirm(confirmation)) return
    mergeItem(token, kind, source.id, target.id)
      .then(() => {
        onDone()
        notify(`« ${source.name} » fusionné dans « ${target.name} ».`, 'success')
        return loadAll()
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Modifier</h2>
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
            <>
              <RenameForm
                key={`${franchise.id}-${franchise.name}`}
                current={franchise.name}
                onRename={(name) => rename('franchises', franchise.id, franchise.name, name)}
              />
              <MergeForm
                key={`merge-${franchise.id}`}
                items={franchises.filter((item) => item.id !== franchise.id)}
                onMerge={(target) =>
                  merge(
                    'franchises',
                    franchise,
                    target,
                    `Fusionner la franchise « ${franchise.name} » dans « ${target.name} » ? Ses jeux passeront dans « ${target.name} » et « ${franchise.name} » sera supprimée.`,
                    () => chooseFranchise(target.id),
                  )
                }
              />
            </>
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
            <>
              <RenameForm
                key={`${game.id}-${game.name}`}
                current={game.name}
                onRename={(name) => rename('games', game.id, game.name, name)}
              />
              <MergeForm
                key={`merge-${game.id}`}
                items={games.filter((item) => item.id !== game.id)}
                renderLabel={(item) => `${item.name} (${item.franchiseName})`}
                getSearchText={(item) => `${item.name} ${item.franchiseName}`}
                onMerge={(target) =>
                  merge(
                    'games',
                    game,
                    target,
                    `Fusionner le jeu « ${game.name} » dans « ${target.name} » (${target.franchiseName}) ? Ses musiques passeront dans « ${target.name} » et « ${game.name} » sera supprimé.`,
                    () => {
                      setFranchiseId(target.franchiseId)
                      setGameId(target.id)
                      setTrackId(null)
                    },
                  )
                }
              />
            </>
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
            <>
              <RenameForm
                key={`${track.id}-${track.name}`}
                current={track.name}
                onRename={(name) => rename('tracks', track.id, track.name, name)}
              />
              <MergeForm
                key={`merge-${track.id}`}
                items={tracks.filter((item) => item.id !== track.id)}
                renderLabel={(item) => `${item.name} (${item.gameName})`}
                getSearchText={(item) => `${item.name} ${item.gameName} ${item.franchiseName}`}
                onMerge={(target) =>
                  merge(
                    'tracks',
                    track,
                    target,
                    `Fusionner « ${track.name} » dans « ${target.name} » (${target.gameName}) ? Ses votes, tags, blindtests et liens manquants passeront à « ${target.name} », puis « ${track.name} » sera supprimée. Si une personne a voté pour les deux, son vote pour « ${target.name} » est gardé.`,
                    () => {
                      setFranchiseId(games.find((item) => item.id === target.gameId)?.franchiseId ?? null)
                      setGameId(target.gameId)
                      setTrackId(target.id)
                    },
                  )
                }
              />
            </>
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

type MergeFormProps<T extends Named> = {
  items: T[]
  renderLabel?: (item: T) => ReactNode
  getSearchText?: (item: T) => string
  onMerge: (target: T) => void
}

function MergeForm<T extends Named>({ items, renderLabel, getSearchText, onMerge }: MergeFormProps<T>) {
  const [target, setTarget] = useState<T | null>(null)

  return (
    <div className="admin-row">
      <NamePicker
        items={items}
        selected={target}
        onSelect={setTarget}
        onClear={() => setTarget(null)}
        placeholder="Fusionner dans…"
        renderLabel={renderLabel}
        getSearchText={getSearchText}
      />
      <button type="button" disabled={target === null} onClick={() => target && onMerge(target)}>
        Fusionner
      </button>
    </div>
  )
}
