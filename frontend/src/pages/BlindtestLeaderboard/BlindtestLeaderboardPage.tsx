import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { fetchLeaderboard, type LeaderboardEntry } from '../../api/blindtests'
import { sortLeaderboard, type LeaderboardSort } from './sortLeaderboard'
import './BlindtestLeaderboardPage.css'

export function BlindtestLeaderboardPage() {
  const { id } = useParams()
  const blindtestId = Number(id)

  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null)
  const [error, setError] = useState(false)
  const [sort, setSort] = useState<LeaderboardSort>('count')

  useEffect(() => {
    const controller = new AbortController()
    fetchLeaderboard(blindtestId, controller.signal)
      .then(setEntries)
      .catch(() => {
        // Un rechargement annule la requête en cours (ce n'est pas une erreur)
        if (!controller.signal.aborted) {
          setError(true)
        }
      })
    return () => controller.abort()
  }, [blindtestId])

  const sorted = entries ? sortLeaderboard(entries, sort) : []

  return (
    <>
      <h1>Classement</h1>
      {error && <p role="alert">Impossible de charger le classement.</p>}
      {!error && entries === null && <p>Chargement...</p>}
      {entries !== null && entries.length === 0 && <p>Personne n'a encore joué.</p>}
      {entries !== null && entries.length > 0 && (
        <>
          <button
            type="button"
            aria-pressed={sort === 'percentage'}
            onClick={() => setSort(sort === 'count' ? 'percentage' : 'count')}
          >
            Trier par {sort === 'count' ? 'pourcentage' : 'nombre de bonnes réponses'}
          </button>
          <ol className="leaderboard">
            {sorted.map((entry) => (
              <li key={entry.listenerName}>
                <span className="name">{entry.listenerName}</span>
                <span className="score">
                  {sort === 'percentage'
                    ? `${entry.tracksHeard > 0 ? Math.round((entry.goodAnswers / entry.tracksHeard) * 100) : 0}%`
                    : `${entry.goodAnswers} bonnes réponses`}
                </span>
                <span className="progress">{entry.tracksHeard} musiques écoutées</span>
              </li>
            ))}
          </ol>
        </>
      )}
    </>
  )
}
