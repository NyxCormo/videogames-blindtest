import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { fetchLeaderboard, type LeaderboardEntry } from '../../api/blindtests'
import { rankLeaderboard, sortLeaderboard, type LeaderboardColumn, type SortDirection } from './sortLeaderboard'
import './BlindtestLeaderboardPage.css'

const COLUMNS: { column: LeaderboardColumn; label: string }[] = [
  { column: 'name', label: 'Joueur' },
  { column: 'good', label: 'Bonnes réponses' },
  { column: 'goodPercent', label: '%' },
  { column: 'franchise', label: 'Franchises' },
  { column: 'franchisePercent', label: '%' },
  { column: 'bonus', label: 'Musiques bonus' },
  { column: 'bonusPercent', label: '%' },
  { column: 'tracksHeard', label: 'Musiques écoutées' },
]

function percentText(count: number, tracksHeard: number): string {
  return `${tracksHeard > 0 ? Math.round((count / tracksHeard) * 100) : 0}%`
}

export function BlindtestLeaderboardPage() {
  const { id } = useParams()
  const blindtestId = Number(id)

  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null)
  const [error, setError] = useState(false)
  // null = pas encore cliqué sur une colonne : on affiche le classement officiel (rankLeaderboard)
  const [sortColumn, setSortColumn] = useState<LeaderboardColumn | null>(null)
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')

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

  function handleSort(column: LeaderboardColumn) {
    if (column === sortColumn) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortColumn(column)
      setSortDirection(column === 'name' ? 'asc' : 'desc')
    }
  }

  const ranked = entries ? rankLeaderboard(entries) : []
  const rankByName = new Map(ranked.map((entry, index) => [entry.listenerName, index + 1]))
  const sorted = entries ? (sortColumn ? sortLeaderboard(entries, sortColumn, sortDirection) : ranked) : []

  return (
    <>
      <h1>Classement</h1>
      {error && <p role="alert">Impossible de charger le classement.</p>}
      {!error && entries === null && <p>Chargement...</p>}
      {entries !== null && entries.length === 0 && <p>Personne n'a encore joué.</p>}
      {entries !== null && entries.length > 0 && (
        <div className="leaderboard-wrap">
          <table className="leaderboard">
            <thead>
              <tr>
                <th>Rang</th>
                {COLUMNS.map(({ column, label }) => (
                  <th key={column}>
                    <button type="button" onClick={() => handleSort(column)}>
                      {label}
                      {sortColumn === column && (sortDirection === 'asc' ? ' ▲' : ' ▼')}
                    </button>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sorted.map((entry) => (
                <tr key={entry.listenerName}>
                  <td>{rankByName.get(entry.listenerName)}</td>
                  <td>{entry.listenerName}</td>
                  <td>{entry.goodAnswers}</td>
                  <td>{percentText(entry.goodAnswers, entry.tracksHeard)}</td>
                  <td>{entry.franchiseAnswers}</td>
                  <td>{percentText(entry.franchiseAnswers, entry.tracksHeard)}</td>
                  <td>{entry.bonusAnswers}</td>
                  <td>{percentText(entry.bonusAnswers, entry.tracksHeard)}</td>
                  <td>{entry.tracksHeard}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
