import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  fetchSession,
  restartBlindtest,
  submitGuess,
  submitGuessFranchise,
  submitKnowAnyway,
  submitPass,
  type BlindtestSession,
  type Reveal,
} from '../../api/blindtests'
import type { Franchise } from '../../api/franchises'
import type { Game } from '../../api/games'
import type { Track } from '../../api/tracks'
import { loadStoredVolume, storeVolume } from '../../audioPreferences'
import { GameGuessForm } from '../../components/GameGuessForm/GameGuessForm'
import { CrossIcon } from '../../components/icons'
import { LoginPrompt } from '../../components/LoginPrompt/LoginPrompt'
import { ReportButton } from '../../components/ReportButton/ReportButton'
import { Stat } from '../../components/Stat/Stat'
import { GameCover, MysteryCover, TrackCard, TrackFacts } from '../../components/TrackCard/TrackCard'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { useNotification } from '../../context/NotificationContext'
import { playWhenAllowed } from '../../playWhenAllowed'
import { plural } from '../../plural'
import { useEnterToContinue } from '../../useEnterToContinue'
import './BlindtestPlayPage.css'

export function BlindtestPlayPage() {
  const { id } = useParams()
  const blindtestId = Number(id)
  const { listener } = useCurrentListener()
  const { notify } = useNotification()

  const [session, setSession] = useState<BlindtestSession | null>(null)
  const [error, setError] = useState(false)
  const [misses, setMisses] = useState<string[]>([])
  const [reveal, setReveal] = useState<Reveal | null>(null)
  const [bonusCorrect, setBonusCorrect] = useState(false)
  // Vrai quand la musique a été révélée sans que le jeu ait été trouvé : passe explicite ou essais épuisés.
  const [revealedAsUnknown, setRevealedAsUnknown] = useState(false)
  const [correctedKnowledge, setCorrectedKnowledge] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const audioRef = useRef<HTMLAudioElement>(null)
  const [confirmRestart, setConfirmRestart] = useState(false)
  const trackId = session?.trackId

  function loadSession() {
    if (!listener) return
    setSession(null)
    fetchSession(blindtestId, listener.id)
      .then(setSession)
      .catch(() => setError(true))
  }

  function refreshAttemptsRemaining() {
    if (!listener) return
    fetchSession(blindtestId, listener.id)
      .then(setSession)
      .catch(() => notify("Le nombre d'essais restants n'a pas pu être mis à jour.", 'error'))
  }

  useEffect(loadSession, [blindtestId, listener])

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    audio.volume = loadStoredVolume()
    return playWhenAllowed(audio)
  }, [trackId])

  useEnterToContinue(reveal !== null, handleNext)

  function handleGuess(game: Game, track: Track | null) {
    if (!listener) return
    setSubmitting(true)
    submitGuess(blindtestId, listener.id, game.id, track ? track.id : null).then((result) => {
      if (result.reveal) {
        setReveal(result.reveal)
        setBonusCorrect(result.bonusCorrect)
        setRevealedAsUnknown(!result.correct)
        setCorrectedKnowledge(false)
      } else {
        setMisses((current) => [...current, `Ce n'est pas ${game.name}`])
        refreshAttemptsRemaining()
      }
    })
      .catch(() => notify("La réponse n'a pas pu être envoyée, il faut réessayer.", 'error'))
      .finally(() => setSubmitting(false))
  }

  function handleGuessFranchise(franchise: Franchise) {
    if (!listener) return
    setSubmitting(true)
    submitGuessFranchise(blindtestId, listener.id, franchise.id).then((result) => {
      if (result.reveal) {
        setReveal(result.reveal)
        setBonusCorrect(false)
        setRevealedAsUnknown(true)
        setCorrectedKnowledge(false)
      } else {
        if (!result.correct) setMisses((current) => [...current, `Ce n'est pas la franchise ${franchise.name}`])
        refreshAttemptsRemaining()
      }
    })
      .catch(() => notify("La réponse n'a pas pu être envoyée, il faut réessayer.", 'error'))
      .finally(() => setSubmitting(false))
  }

  function handlePass() {
    if (!listener) return
    setSubmitting(true)
    submitPass(blindtestId, listener.id).then((result) => {
      setReveal(result)
      setBonusCorrect(false)
      setRevealedAsUnknown(true)
      setCorrectedKnowledge(false)
    })
      .catch(() => notify('Impossible de passer cette musique, il faut réessayer.', 'error'))
      .finally(() => setSubmitting(false))
  }

  function handleKnowAnyway() {
    if (!listener || !reveal) return
    submitKnowAnyway(blindtestId, listener.id, reveal.trackId)
      .then(() => setCorrectedKnowledge(true))
      .catch(() => notify("La correction n'a pas pu être enregistrée, il faut réessayer.", 'error'))
  }

  function handleNext() {
    setMisses([])
    setReveal(null)
    setBonusCorrect(false)
    setRevealedAsUnknown(false)
    setCorrectedKnowledge(false)
    loadSession()
  }

  function handleRestart() {
    if (!listener) return
    setConfirmRestart(false)
    restartBlindtest(blindtestId, listener.id)
      .then(loadSession)
      .catch(() => notify("La partie n'a pas pu être recommencée, il faut réessayer.", 'error'))
  }

  if (!listener) {
    return <LoginPrompt message="Il faut un pseudo pour jouer." />
  }
  if (error) {
    return <p role="alert">Impossible de charger la partie.</p>
  }
  if (session === null) {
    return <p>Chargement...</p>
  }
  if (session.finished) {
    const percent = (count: number) => `${Math.round((count / Math.max(session.totalTracks, 1)) * 100)} %`
    return (
      <>
        <h1>Partie terminée</h1>
        <div className="blindtest-stage blindtest-summary">
          <div className="blindtest-scores">
            <Stat value={`${session.goodAnswers} / ${session.totalTracks}`} label={`Jeux trouvés (${percent(session.goodAnswers)})`} />
            <Stat value={`${session.franchiseAnswers} / ${session.totalTracks}`} label={`Franchises trouvées (${percent(session.franchiseAnswers)})`} />
            <Stat value={`${session.bonusAnswers} / ${session.totalTracks}`} label={`Musiques bonus (${percent(session.bonusAnswers)})`} />
          </div>
          <div className="blindtest-summary-actions">
            <Link className="button button-primary" to={`/blindtests/${blindtestId}/leaderboard`}>
              Voir le classement
            </Link>
            {confirmRestart ? (
              <>
                <span>Le score actuel sera remplacé.</span>
                <button type="button" onClick={handleRestart}>
                  Rejouer quand même
                </button>
                <button type="button" onClick={() => setConfirmRestart(false)}>
                  Annuler
                </button>
              </>
            ) : (
              <button type="button" onClick={() => setConfirmRestart(true)}>
                Rejouer
              </button>
            )}
          </div>
        </div>
      </>
    )
  }

  return (
    <>
      <h1>Blindtest</h1>
      <p className="progress">
        {session.tracksHeard + 1} / {session.totalTracks} musiques &middot;{' '}
        {plural(session.goodAnswers, 'bonne réponse', 'bonnes réponses')}
      </p>

      <div className="blindtest-stage">
        <TrackCard
          result={reveal ? (revealedAsUnknown ? 'failure' : 'success') : null}
          media={
            <>
              {reveal ? <GameCover gameId={reveal.gameId} gameName={reveal.gameName} coverUrl={reveal.coverUrl} /> : <MysteryCover />}
              <audio
                ref={audioRef}
                controls
                loop
                src={session.audioLink ?? undefined}
                onVolumeChange={(event) => storeVolume(event.currentTarget.volume)}
              />
              {!reveal && (
                <>
                  <p className="blindtest-attempts">
                    <span className="blindtest-dots" aria-hidden="true">
                      {Array.from({ length: session.maxAttempts }, (_, index) => (
                        <span
                          key={index}
                          className={`blindtest-dot${index < session.attemptsRemaining ? ' blindtest-dot-left' : ''}`}
                        />
                      ))}
                    </span>
                    {plural(session.attemptsRemaining, 'essai restant', 'essais restants')}
                  </p>
                  {misses.length > 0 && (
                    <ul className="blindtest-misses" role="alert">
                      {misses.map((miss, index) => (
                        <li key={index}>
                          <CrossIcon /> {miss}
                        </li>
                      ))}
                    </ul>
                  )}
                  <button type="button" onClick={handlePass} disabled={submitting}>
                    Je passe
                  </button>
                </>
              )}
            </>
          }
          details={
            reveal && (
              <>
                <TrackFacts
                  success={!revealedAsUnknown}
                  title={revealedAsUnknown ? (correctedKnowledge ? 'Musique révélée, notée connue' : 'Musique révélée') : 'Bien joué !'}
                  franchise={reveal.franchiseName}
                  game={reveal.gameName}
                  track={reveal.trackName}
                />
                {bonusCorrect && <p className="bonus">+ bonus musique trouvée !</p>}
                {revealedAsUnknown && !correctedKnowledge && (
                  <button type="button" onClick={handleKnowAnyway}>
                    Je connais en fait
                  </button>
                )}
                <Link to={`/tracks/${reveal.trackId}`} target="_blank">
                  Voir la fiche de la musique
                </Link>
                <ReportButton trackId={reveal.trackId} />
              </>
            )
          }
        />

        {reveal ? (
          <button type="button" className="button-primary track-next" onClick={handleNext} autoFocus>
            Suivant
          </button>
        ) : (
          <div className="track-search">
            <GameGuessForm
              key={`${session.trackId}-${session.attemptsRemaining}`}
              foundFranchiseId={session.foundFranchiseId}
              onSubmit={handleGuess}
              onGuessFranchise={handleGuessFranchise}
              disabled={submitting}
            />
          </div>
        )}
      </div>
    </>
  )
}
