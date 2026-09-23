import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { createBlindtest } from '../../api/blindtests'
import './BlindtestCreatePage.css'

export function BlindtestCreatePage() {
  const [name, setName] = useState('')
  const [trackCount, setTrackCount] = useState(10)
  const [difficulty, setDifficulty] = useState(50)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    createBlindtest(name, trackCount, difficulty)
      .then(() => navigate('/blindtests'))
      .catch((err: Error) => setError(err.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <>
      <h1>Créer un blindtest</h1>
      <form onSubmit={handleSubmit} className="blindtest-create">
        <label>
          Nom
          <input
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>
        <label>
          Nombre de musiques
          <input
            type="number"
            min={1}
            value={trackCount}
            onChange={(event) => setTrackCount(Number(event.target.value))}
            required
          />
        </label>
        <label>
          Difficulté ({difficulty})
          <input
            type="range"
            min={0}
            max={100}
            value={difficulty}
            onChange={(event) => setDifficulty(Number(event.target.value))}
          />
        </label>
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Création...' : 'Créer'}
        </button>
      </form>
    </>
  )
}
