import { useEffect, useMemo, useState } from 'react'
import { fetchGameCover, fetchGameCovers, saveGameCover } from '../../api/admin'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { EntityList } from '../../components/EntityList/EntityList'
import { NoImageIcon } from '../../components/icons'
import { Select } from '../../components/Select/Select'
import { useNotification } from '../../context/NotificationContext'
import { plural } from '../../plural'
import { CoverCandidates } from './CoverCandidates'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type CoverFilter = 'all' | 'with' | 'without'

const FILTERS: { value: CoverFilter; label: string }[] = [
  { value: 'all', label: 'Tous les jeux' },
  { value: 'with', label: 'Jeux avec image' },
  { value: 'without', label: 'Jeux sans image' },
]

type Props = {
  token: string
  onSessionExpired: () => void
  onChangeCover: (game: Game) => void
  onCoverChanged: () => void
}

export function GameCoversList({ token, onSessionExpired, onChangeCover, onCoverChanged }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [covers, setCovers] = useState<Map<number, string>>(new Map())
  const [filter, setFilter] = useState<CoverFilter>('all')
  const [franchiseId, setFranchiseId] = useState<number | null>(null)
  const [gameId, setGameId] = useState<number | null>(null)
  const [coverUrl, setCoverUrl] = useState<{ gameId: number; value: string | null } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([fetchFranchises(controller.signal), fetchGames(controller.signal), fetchGameCovers(token)])
      .then(([loadedFranchises, loadedGames, loadedCovers]) => {
        setFranchises(loadedFranchises)
        setGames(loadedGames)
        setCovers(new Map(loadedCovers.map((entry) => [entry.gameId, entry.displayUrl])))
      })
      .catch((err: Error) => {
        if (!controller.signal.aborted) handleError(err)
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const shownGames = useMemo(
    () => games.filter((game) => filter === 'all' || (filter === 'with') === covers.has(game.id)),
    [games, covers, filter],
  )
  const gamesByFranchise = useMemo(() => {
    const groups = new Map<number, Game[]>()
    for (const game of shownGames) {
      const group = groups.get(game.franchiseId)
      if (group) {
        group.push(game)
      } else {
        groups.set(game.franchiseId, [game])
      }
    }
    return groups
  }, [shownGames])

  const shownFranchises = franchises && franchises.filter((franchise) => gamesByFranchise.has(franchise.id))
  const franchise = shownFranchises?.find((item) => item.id === franchiseId)
  const franchiseGames = franchise ? (gamesByFranchise.get(franchise.id) ?? []) : null
  const game = franchiseGames?.find((item) => item.id === gameId)
  const cover = game ? covers.get(game.id) : undefined

  useEffect(() => {
    if (gameId === null) return
    fetchGameCover(token, gameId)
      .then((value) => setCoverUrl({ gameId, value: value.coverUrl }))
      .catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gameId])

  function save(target: Game, image: string) {
    saveGameCover(token, target.id, image)
      .then((value) => {
        setCoverUrl({ gameId: target.id, value: value.coverUrl })
        setCovers((previous) => {
          const next = new Map(previous)
          if (value.displayUrl) next.set(target.id, value.displayUrl)
          return next
        })
        onCoverChanged()
        notify(
          value.downloaded
            ? `Image de « ${target.name} » enregistrée et téléchargée.`
            : `Image de « ${target.name} » enregistrée, mais pas téléchargée : l'adresse d'origine sera utilisée.`,
          'success',
        )
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Jeux et images</h2>
      <div className="admin-row">
        <Select ariaLabel="Filtrer les jeux" options={FILTERS} value={filter} onChange={setFilter} />
        <span className="muted">
          {plural(covers.size, 'jeu avec image', 'jeux avec image')} sur {games.length}
        </span>
      </div>
      <div className="entity-columns">
        <EntityList
          key={`franchises-${filter}`}
          title="Franchises"
          items={shownFranchises}
          label={(item) => item.name}
          detail={(item) => plural(gamesByFranchise.get(item.id)?.length ?? 0, 'jeu', 'jeux')}
          activeId={franchiseId}
          onOpen={(item) => {
            setFranchiseId(item.id)
            setGameId(null)
          }}
          emptyText="Aucune franchise pour ce filtre."
        />
        {franchiseGames === null ? (
          <p className="muted entity-column-hint">Choisir une franchise pour voir ses jeux.</p>
        ) : (
          <EntityList
            key={`jeux-${franchiseId}-${filter}`}
            title={`Jeux de ${franchise?.name ?? ''}`}
            items={franchiseGames}
            label={(item) => item.name}
            activeId={gameId}
            onOpen={(item) => setGameId(item.id)}
            emptyText="Aucun jeu pour ce filtre."
          />
        )}
        {!game ? (
          <p className="muted entity-column-hint">Choisir un jeu pour voir son image.</p>
        ) : (
          <div className="entity-list admin-cover-panel">
            <h3>Image de {game.name}</h3>
            {cover ? (
              <img src={cover} alt={`Image de ${game.name}`} />
            ) : (
              <div className="admin-cover-panel-empty" role="img" aria-label="Pas d'image pour ce jeu">
                <NoImageIcon />
                <span>Pas d'image</span>
              </div>
            )}
            <button type="button" onClick={() => onChangeCover(game)}>
              Saisir une autre adresse
            </button>
          </div>
        )}
      </div>
      {game && (
        <div className="admin-cover-panel-candidates">
          <h3>Pochettes des albums de {game.name}</h3>
          <CoverCandidates
            token={token}
            onSessionExpired={onSessionExpired}
            game={game}
            currentUrl={coverUrl?.gameId === game.id ? coverUrl.value : null}
            onChoose={(image) => save(game, image)}
          />
        </div>
      )}
    </section>
  )
}
