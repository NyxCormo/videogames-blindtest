import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import {
  fetchSession,
  submitGuess,
  submitKnowAnyway,
  submitPass,
  type BlindtestSession,
  type Reveal,
} from '../../api/blindtests'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import './BlindtestPlayPage.css'

export function BlindtestPlayPage() {
  const { id } = useParams()
  const blindtestId = Number(id)
  const { listener } = useCurrentListener()

  const [session, setSession] = useState<BlindtestSession | null>(null)
  const [error, setError] = useState(false)
  const [guess, setGuess] = useState('')
  const [wrongGuess, setWrongGuess] = useState(false)
  const [reveal, setReveal] = useState<Reveal | null>(null)
  const [passed, setPassed] = useState(false)
  const [correctedKnowledge, setCorrectedKnowledge] = useState(false)

  function loadSession() {
    if (!listener) return
    setSession(null)
    fetchSession(blindtestId, listener.id)
      .then(setSession)
      .catch(() => setError(true))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadSession, [blindtestId, listener])

  function handleGuess(event: FormEvent) {
    event.preventDefault()
    if (!listener) return
    submitGuess(blindtestId, listener.id, guess).then((result) => {
      if (result.correct && result.reveal) {
        setReveal(result.reveal)
        setPassed(false)
      } else {
        setWrongGuess(true)
      }
    })
  }

  function handlePass() {
    if (!listener) return
    submitPass(blindtestId, listener.id).then((result) => {
      setReveal(result)
      setPassed(true)
      setCorrectedKnowledge(false)
    })
  }

  function handleKnowAnyway() {
    if (!listener || !reveal) return
    submitKnowAnyway(blindtestId, listener.id, reveal.trackId).then(() => setCorrectedKnowledge(true))
  }

  function handleNext() {
    setGuess('')
    setWrongGuess(false)
    setReveal(null)
    setPassed(false)
    setCorrectedKnowledge(false)
    loadSession()
  }

  if (!listener) {
    return <p>Connecte-toi pour jouer.</p>
  }
  if (error) {
    return <p role="alert">Impossible de charger la partie.</p>
  }
  if (session === null) {
    return <p>Chargement...</p>
  }
  if (session.finished) {
    return (
      <>
        <h1>Partie terminée</h1>
        <p>
          {session.goodAnswers} / {session.totalTracks} bonnes réponses.
        </p>
        <p>
          <Link to={`/blindtests/${blindtestId}/leaderboard`}>Voir le classement</Link>
        </p>
      </>
    )
  }

  return (
    <>
      <h1>Blindtest</h1>
      <p className="progress">
        {session.tracksHeard} / {session.totalTracks} musiques &middot; {session.goodAnswers} bonnes réponses
      </p>
      <audio key={session.trackId} controls src={session.audioLink ?? undefined} />

      {reveal ? (
        <div className="reveal">
          <p>
            <strong>{reveal.franchiseName}</strong> — {reveal.gameName} — {reveal.trackName}
          </p>
          {passed && !correctedKnowledge && (
            <button type="button" onClick={handleKnowAnyway}>
              Ah, je connais en fait
            </button>
          )}
          <button type="button" onClick={handleNext}>
            Suivant
          </button>
        </div>
      ) : (
        <form onSubmit={handleGuess} className="guess-form">
          <input
            type="text"
            placeholder="Nom du jeu"
            value={guess}
            onChange={(event) => {
              setGuess(event.target.value)
              setWrongGuess(false)
            }}
            required
            autoFocus
          />
          <button type="submit">Valider</button>
          <button type="button" onClick={handlePass}>
            Je passe
          </button>
          {wrongGuess && <p role="alert">Ce n'est pas ça, réessaie.</p>}
        </form>
      )}
    </>
  )
}
