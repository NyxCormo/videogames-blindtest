import { useEffect, useMemo, useState } from 'react'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { fetchTracks, type Track } from '../../api/tracks'
import { plural } from './adminText'
import { EntityList } from './EntityList'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  onSessionExpired: () => void
}

function groupBy<T>(items: T[], key: (item: T) => number): Map<number, T[]> {
  const groups = new Map<number, T[]>()
  for (const item of items) {
    const group = groups.get(key(item))
    if (group) {
      group.push(item)
    } else {
      groups.set(key(item), [item])
    }
  }
  return groups
}

export function CatalogTab({ onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [franchiseId, setFranchiseId] = useState<number | null>(null)
  const [gameId, setGameId] = useState<number | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([fetchFranchises(controller.signal), fetchGames(controller.signal), fetchTracks(controller.signal)])
      .then(([loadedFranchises, loadedGames, loadedTracks]) => {
        setFranchises(loadedFranchises)
        setGames(loadedGames)
        setTracks(loadedTracks)
      })
      .catch((err: Error) => {
        if (!controller.signal.aborted) handleError(err)
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const gamesByFranchise = useMemo(() => groupBy(games, (game) => game.franchiseId), [games])
  const tracksByGame = useMemo(() => groupBy(tracks, (track) => track.gameId), [tracks])

  function franchiseDetail(franchise: Franchise): string {
    const franchiseGames = gamesByFranchise.get(franchise.id) ?? []
    const trackCount = franchiseGames.reduce((total, game) => total + (tracksByGame.get(game.id)?.length ?? 0), 0)
    return `${plural(franchiseGames.length, 'jeu', 'jeux')} · ${plural(trackCount, 'musique', 'musiques')}`
  }

  const franchiseGames = franchiseId === null ? null : (gamesByFranchise.get(franchiseId) ?? [])
  const gameTracks = gameId === null ? null : (tracksByGame.get(gameId) ?? [])
  const franchise = franchises?.find((item) => item.id === franchiseId)
  const game = games.find((item) => item.id === gameId)

  return (
    <div className="admin-columns">
      <EntityList
        title="Franchises"
        items={franchises}
        label={(item) => item.name}
        detail={franchiseDetail}
        activeId={franchiseId}
        onOpen={(item) => {
          setFranchiseId(item.id)
          setGameId(null)
        }}
        emptyText="Aucune franchise."
      />
      {franchiseGames === null ? (
        <p className="admin-muted admin-column-hint">Choisir une franchise pour voir ses jeux.</p>
      ) : (
        <EntityList
          key={`jeux-${franchiseId}`}
          title={`Jeux de ${franchise?.name ?? ''}`}
          items={franchiseGames}
          label={(item) => item.name}
          detail={(item) => plural(tracksByGame.get(item.id)?.length ?? 0, 'musique', 'musiques')}
          activeId={gameId}
          onOpen={(item) => setGameId(item.id)}
          emptyText="Aucun jeu dans cette franchise."
        />
      )}
      {gameTracks === null ? (
        <p className="admin-muted admin-column-hint">Choisir un jeu pour voir ses musiques.</p>
      ) : (
        <EntityList
          key={`musiques-${gameId}`}
          title={`Musiques de ${game?.name ?? ''}`}
          items={gameTracks}
          label={(item) => item.name}
          emptyText="Aucune musique dans ce jeu."
        />
      )}
    </div>
  )
}
