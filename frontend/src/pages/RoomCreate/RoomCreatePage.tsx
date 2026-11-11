import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { fetchBlindtests, type Blindtest } from '../../api/blindtests'
import { createRoom, type RoomMode, type RoomVisibility, type ScoringMode } from '../../api/rooms'
import { ChoiceGroup, type Choice } from '../../components/ChoiceGroup/ChoiceGroup'
import { LoginPrompt } from '../../components/LoginPrompt/LoginPrompt'
import { Select } from '../../components/Select/Select'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { MODE_LABELS, PLAYBACK_LABELS, SCORING_LABELS, VISIBILITY_LABELS } from '../Room/roomLabels'
import './RoomCreatePage.css'

const VISIBILITY_CHOICES: Choice<RoomVisibility>[] = [
  { value: 'PUBLIC', label: VISIBILITY_LABELS.PUBLIC, hint: 'La salle apparaît dans la liste des salles et dans un bandeau sur tout le site.' },
  { value: 'PRIVATE', label: VISIBILITY_LABELS.PRIVATE, hint: 'La salle se rejoint uniquement avec son code.' },
]

const MODE_CHOICES: Choice<RoomMode>[] = [
  { value: 'MULTI', label: MODE_LABELS.MULTI, hint: 'Chaque joueur entend la musique sur son appareil. Révélation et musique suivante automatiques.' },
  { value: 'PRESENTATION', label: MODE_LABELS.PRESENTATION, hint: "La musique passe seulement sur l'écran de la personne qui crée la salle. Elle anime sans jouer, révèle et lance la musique suivante quand elle veut." },
]

const PLAYBACK_CHOICES: Choice<keyof typeof PLAYBACK_LABELS>[] = [
  { value: 'FREE', label: PLAYBACK_LABELS.FREE, hint: 'Chaque joueur peut mettre en pause, revenir en arrière ou avancer dans la musique.' },
  { value: 'SYNCED', label: PLAYBACK_LABELS.SYNCED, hint: 'La musique avance au même rythme pour tous, seul le volume se règle.' },
]

const SCORING_CHOICES: Choice<ScoringMode>[] = [
  { value: 'SPEED', label: SCORING_LABELS.SPEED, hint: 'Un jeu trouvé rapporte de 1000 à 500 points selon le temps restant.' },
  { value: 'FIRST_COME', label: SCORING_LABELS.FIRST_COME, hint: 'Un jeu trouvé rapporte 1000, 800, 600, 400 puis 200 points selon l\'ordre d\'arrivée.' },
]

export function RoomCreatePage() {
  const { listener } = useCurrentListener()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [blindtests, setBlindtests] = useState<Blindtest[] | null>(null)
  const [blindtestId, setBlindtestId] = useState(searchParams.get('blindtest') ?? '')
  const [visibility, setVisibility] = useState<RoomVisibility>('PUBLIC')
  const [mode, setMode] = useState<RoomMode>('MULTI')
  const [playback, setPlayback] = useState<keyof typeof PLAYBACK_LABELS>('FREE')
  const [scoringMode, setScoringMode] = useState<ScoringMode>('SPEED')
  const [answerSeconds, setAnswerSeconds] = useState(30)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    fetchBlindtests(controller.signal)
      .then((loaded) => {
        setBlindtests(loaded)
        setBlindtestId((current) => current || (loaded.length > 0 ? String(loaded[0].id) : ''))
      })
      .catch(() => {
        if (!controller.signal.aborted) setError('Impossible de charger les blindtests.')
      })
    return () => controller.abort()
  }, [])

  if (!listener) {
    return <LoginPrompt message="Un pseudo est nécessaire pour créer une salle." />
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!listener) return
    setError(null)
    setSubmitting(true)
    createRoom(listener.id, {
      blindtestId: Number(blindtestId),
      visibility,
      mode,
      playersCanSeek: playback === 'FREE',
      scoringMode,
      answerSeconds,
    })
      .then((room) => navigate(`/rooms/${room.code}`))
      .catch((err: Error) => {
        setError(err.message)
        setSubmitting(false)
      })
  }

  return (
    <>
      <h1>Créer une salle</h1>
      {blindtests !== null && blindtests.length === 0 && (
        <p>
          Aucun blindtest pour l'instant. <Link to="/blindtests/new">Créer un blindtest</Link>
        </p>
      )}
      {blindtests !== null && blindtests.length > 0 && (
        <form onSubmit={handleSubmit} className="room-create">
          <div className="room-create-field">
            Blindtest
            <Select
              ariaLabel="Blindtest"
              options={blindtests.map((blindtest) => ({ value: String(blindtest.id), label: blindtest.name }))}
              value={blindtestId}
              onChange={setBlindtestId}
            />
          </div>
          <ChoiceGroup legend="Visibilité" choices={VISIBILITY_CHOICES} value={visibility} onChange={setVisibility} />
          <ChoiceGroup legend="Mode" choices={MODE_CHOICES} value={mode} onChange={setMode} />
          {mode === 'MULTI' && (
            <ChoiceGroup legend="Lecture" choices={PLAYBACK_CHOICES} value={playback} onChange={setPlayback} />
          )}
          <ChoiceGroup legend="Points" choices={SCORING_CHOICES} value={scoringMode} onChange={setScoringMode} />
          <label className="room-create-field">
            Temps de réponse (secondes)
            <input
              type="number"
              min={10}
              max={120}
              value={answerSeconds}
              onChange={(event) => setAnswerSeconds(Number(event.target.value))}
              required
            />
          </label>
          {error && <p role="alert">{error}</p>}
          <button type="submit" className="button button-primary" disabled={submitting}>
            Créer la salle
          </button>
        </form>
      )}
      {blindtests === null && !error && <p>Chargement...</p>}
      {blindtests === null && error && <p role="alert">{error}</p>}
    </>
  )
}
