import { useEffect, useState } from 'react'
import { fetchFranchises, type Franchise } from '../../api/franchises'
import { fetchGames, type Game } from '../../api/games'
import { fetchTracks, type Track } from '../../api/tracks'
import './EnrichmentPage.css'

export function EnrichmentPage() {
  const [franchises, setFranchises] = useState<Franchise[] | null>(null)
  const [games, setGames] = useState<Game[]>([])
  const [tracks, setTracks] = useState<Track[]>([])
  const [error, setError] = useState(false)

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

  if (error) {
    return <p role="alert">Impossible de charger les données.</p>
  }
  if (franchises === null) {
    return <p>Chargement...</p>
  }

  return (
    <>
      <h1>Enrichissement</h1>
      <div className="enrichment-tree">
        {franchises.map((franchise) => {
          const franchiseGames = games.filter((game) => game.franchiseId === franchise.id)
          const franchiseTrackCount = franchiseGames.reduce(
            (total, game) => total + tracks.filter((track) => track.gameId === game.id).length,
            0,
          )
          return (
            <details key={franchise.id} className="enrichment-franchise" open>
              <summary>
                {franchise.name} ({franchiseGames.length}) ({franchiseTrackCount})
              </summary>
              <div className="enrichment-games">
                {franchiseGames.map((game) => {
                  const gameTracks = tracks.filter((track) => track.gameId === game.id)
                  return (
                    <details key={game.id} className="enrichment-game">
                      <summary>
                        {game.name} ({gameTracks.length})
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
