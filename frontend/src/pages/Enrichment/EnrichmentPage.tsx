import { useEffect, useMemo, useState } from 'react'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { fetchTracks, type Track } from '../../api/tracks'
import { filterEnrichment } from './filterEnrichment'
import './EnrichmentPage.css'

const MAX_TRACKS_PER_GAME = 25

export function EnrichmentPage() {
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [error, setError] = useState(false)
  const [query, setQuery] = useState('')
  const [onlyFranchisesWithoutGames, setOnlyFranchisesWithoutGames] = useState(false)
  const [onlyGamesWithoutTracks, setOnlyGamesWithoutTracks] = useState(false)
  const [maxTracksPerGame, setMaxTracksPerGame] = useState(MAX_TRACKS_PER_GAME)

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      fetchFranchises(controller.signal),
      fetchGames(controller.signal),
      fetchTracks(controller.signal),
    ])
      .then(([allFranchises, allGames, allTracks]) => {
        setFranchises(allFranchises)
        setGames(allGames)
        setTracks(allTracks)
      })
      .catch(() => {
        if (!controller.signal.aborted) setError(true)
      })
    return () => controller.abort()
  }, [])

  const shown = useMemo(
    () =>
      franchises
        ? filterEnrichment(
            franchises,
            games,
            tracks,
            query,
            onlyFranchisesWithoutGames,
            onlyGamesWithoutTracks,
            maxTracksPerGame,
          )
        : [],
    [franchises, games, tracks, query, onlyFranchisesWithoutGames, onlyGamesWithoutTracks, maxTracksPerGame],
  )

  if (error) {
    return <p role="alert">Impossible de charger les données.</p>
  }
  if (franchises === null) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <h1>Enrichissement</h1>
      <button type="button" className="enrichment-add-franchise">
        Ajouter une franchise
      </button>
      <div className="controls">
        <input
          type="search"
          placeholder="Rechercher une franchise ou un jeu"
          aria-label="Rechercher"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <label>
          <input
            type="checkbox"
            checked={onlyFranchisesWithoutGames}
            onChange={(event) => setOnlyFranchisesWithoutGames(event.target.checked)}
          />
          Franchises sans jeu uniquement
        </label>
        <label>
          <input
            type="checkbox"
            checked={onlyGamesWithoutTracks}
            onChange={(event) => setOnlyGamesWithoutTracks(event.target.checked)}
          />
          Jeux sans musique uniquement
        </label>
        <label className="enrichment-slider">
          Jeux avec au plus {maxTracksPerGame} musique{maxTracksPerGame > 1 ? 's' : ''}
          <input
            type="range"
            min={0}
            max={MAX_TRACKS_PER_GAME}
            value={maxTracksPerGame}
            onChange={(event) => setMaxTracksPerGame(Number(event.target.value))}
          />
        </label>
      </div>
      <div className="enrichment-tree">
        {shown.map(({ franchise, games: franchiseGames }) => {
          const totalGames = games.filter((game) => game.franchiseId === franchise.id).length
          const totalTracks = games
            .filter((game) => game.franchiseId === franchise.id)
            .reduce((total, game) => total + tracks.filter((track) => track.gameId === game.id).length, 0)
          return (
            <details key={franchise.id} className="enrichment-franchise" open>
              <summary>
                <span>
                  {franchise.name} ({totalGames}) ({totalTracks})
                </span>
                <button type="button" onClick={(event) => event.stopPropagation()}>
                  Ajouter un jeu
                </button>
              </summary>
              <div className="enrichment-games">
                {franchiseGames.map((game) => {
                  const gameTracks = tracks.filter((track) => track.gameId === game.id)
                  return (
                    <details key={game.id} className="enrichment-game">
                      <summary>
                        <span>
                          {game.name} ({gameTracks.length})
                        </span>
                        <button type="button" onClick={(event) => event.stopPropagation()}>
                          Ajouter une musique
                        </button>
                      </summary>
                      <ul className="enrichment-tracks">
                        {gameTracks.map((track) => (
                          <li key={track.id}>{track.name}</li>
                        ))}
                      </ul>
                    </details>
                  )
                })}
              </div>
            </details>
          )
        })}
      </div>
    </>
  )
}
