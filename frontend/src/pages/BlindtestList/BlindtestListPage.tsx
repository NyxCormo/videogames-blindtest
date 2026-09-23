import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { fetchBlindtests, type Blindtest } from '../../api/blindtests'
import './BlindtestListPage.css'

export function BlindtestListPage() {
  const [blindtests, setBlindtests] = useState<Blindtest[] | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    fetchBlindtests(controller.signal)
      .then(setBlindtests)
      .catch(() => {
        // Un rechargement annule la requête en cours (ce n'est pas une erreur)
        if (!controller.signal.aborted) {
          setError(true)
        }
      })
    return () => controller.abort()
  }, [])

  return (
    <>
      <h1>Blindtests</h1>
      <p><Link to="/blindtests/new">Créer un blindtest</Link></p>
      {error && <p role="alert">Impossible de charger les blindtests. Le backend est-il lancé ?</p>}
      {!error && blindtests === null && <p>Chargement...</p>}
      {blindtests !== null && blindtests.length === 0 && <p>Aucun blindtest pour l'instant.</p>}
      {blindtests !== null && blindtests.length > 0 && (
        <ul className="blindtest-list">
          {blindtests.map((blindtest) => (
            <li key={blindtest.id}>
              <span className="name">{blindtest.name}</span>
              <span className="difficulty">Difficulté {blindtest.difficulty}</span>
              <span className="date">{new Date(blindtest.createdAt).toLocaleString('fr-FR')}</span>
              <Link to={`/blindtests/${blindtest.id}/play`}>Jouer</Link>
              <Link to={`/blindtests/${blindtest.id}/leaderboard`}>Classement</Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
