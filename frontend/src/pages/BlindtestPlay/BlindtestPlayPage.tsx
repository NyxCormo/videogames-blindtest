import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  fetchSession,
  submitGuess,
  submitGuessFranchise,
  submitKnowAnyway,
  submitPass,
  type BlindtestSession,
  type Reveal,
} from '../../api/blindtests'
import { loadStoredVolume, storeVolume } from '../../audioPreferences'
import { GameGuessForm } from '../../components/GameGuessForm/GameGuessForm'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import './BlindtestPlayPage.css'

export function BlindtestPlayPage() {
  const { id } = useParams()
  const blindtestId = Number(id)
  const { listener } = useCurrentListener()

  const [session, setSession] = useState<BlindtestSession | null>(null)
  const [error, setError] = useState(false)
  const [wrongGuess, setWrongGuess] = useState(false)
  const [reveal, setReveal] = useState<Reveal | null>(null)
  const [bonusCorrect, setBonusCorrect] = useState(false)
  // Vrai quand la musique a été révélée sans que le jeu ait été trouvé : passe explicite ou essais épuisés.
  const [revealedAsUnknown, setRevealedAsUnknown] = useState(false)
  const [correctedKnowledge, setCorrectedKnowledge] = useState(false)

  function loadSession() {
    if (!listener) return
    setSession(null)
    fetchSession(blindtestId, listener.id)
      .then(setSession)
      .catch(() => setError(true))
  }

  // Comme loadSession, mais sans repasser par "session = null" entre-temps : utilisée après une tentative
  // manquée sur la même musique, pour rafraîchir juste les essais restants sans démonter GameGuessForm
  // (ça lui ferait perdre sa sélection en cours et le panneau "franchise" ouvert).
  function refreshAttemptsRemaining() {
    if (!listener) return
    fetchSession(blindtestId, listener.id)
      .then(setSession)
      .catch(() => {})
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadSession, [blindtestId, listener])

  function handleGuess(gameId: number, trackId: number | null) {
    if (!listener) return
    setWrongGuess(false)
    submitGuess(blindtestId, listener.id, gameId, trackId).then((result) => {
      if (result.reveal) {
        setReveal(result.reveal)
        setBonusCorrect(result.bonusCorrect)
        setRevealedAsUnknown(!result.correct)
        setCorrectedKnowledge(false)
      } else {
        setWrongGuess(true)
        refreshAttemptsRemaining()
      }
    })
  }

  function handleGuessFranchise(franchiseId: number): Promise<{ correct: boolean; revealed: boolean }> {
    if (!listener) return Promise.resolve({ correct: false, revealed: false })
    return submitGuessFranchise(blindtestId, listener.id, franchiseId).then((result) => {
      if (result.reveal) {
        setReveal(result.reveal)
        setBonusCorrect(false)
        setRevealedAsUnknown(true)
        setCorrectedKnowledge(false)
      } else {
        refreshAttemptsRemaining()
      }
      return { correct: result.correct, revealed: result.reveal !== null }
    })
  }

  function handlePass() {
    if (!listener) return
    submitPass(blindtestId, listener.id).then((result) => {
      setReveal(result)
      setBonusCorrect(false)
      setRevealedAsUnknown(true)
      setCorrectedKnowledge(false)
    })
  }

  function handleKnowAnyway() {
    if (!listener || !reveal) return
    submitKnowAnyway(blindtestId, listener.id, reveal.trackId).then(() => setCorrectedKnowledge(true))
  }

  function handleNext() {
    setWrongGuess(false)
    setReveal(null)
    setBonusCorrect(false)
    setRevealedAsUnknown(false)
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
        &middot; {session.attemptsRemaining} essai{session.attemptsRemaining > 1 ? 's' : ''} restant
        {session.attemptsRemaining > 1 ? 's' : ''}
      </p>
      {/* ref-callback plutôt qu'une ref + un effet : "Suivant" démonte cette page pendant son
          chargement (session repasse par null), donc l'élément est recréé à chaque musique. Une
          ref-callback s'exécute à chaque création, un effet à dépendances vides une seule fois. */}
      <audio
        ref={(element) => {
          if (element) {
            element.volume = loadStoredVolume()
          }
        }}
        controls
        src={session.audioLink ?? undefined}
        onVolumeChange={(event) => storeVolume(event.currentTarget.volume)}
      />

      {reveal ? (
        <div className="reveal">
          <p>
            <strong>{reveal.franchiseName}</strong> — {reveal.gameName} — {reveal.trackName}
          </p>
          {bonusCorrect && <p className="bonus">+ bonus musique trouvée !</p>}
          {revealedAsUnknown && !correctedKnowledge && (
            <button type="button" onClick={handleKnowAnyway}>
              Ah, je connais en fait
            </button>
          )}
          <button type="button" onClick={handleNext}>
            Suivant
          </button>
        </div>
      ) : (
        <>
          <GameGuessForm
            key={session.trackId}
            onSubmit={handleGuess}
            onGuessFranchise={handleGuessFranchise}
            onPass={handlePass}
          />
          {wrongGuess && <p role="alert">Ce n'est pas ça, réessaie.</p>}
        </>
      )}
    </>
  )
}
