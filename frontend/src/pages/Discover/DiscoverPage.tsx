import { useEffect, useRef, useState, type PointerEvent } from 'react'
import { Link } from 'react-router'
import { fetchNextTrack, setKnowledge, type DiscoverTrack } from '../../api/discover'
import { fetchGames, type Game } from '../../api/games'
import { loadStoredVolume, storeVolume } from '../../audioPreferences'
import { CheckIcon, CrossIcon } from '../../components/icons'
import { LoginPrompt } from '../../components/LoginPrompt/LoginPrompt'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { ReportButton } from '../../components/ReportButton/ReportButton'
import { GameCover, MysteryCover, TrackCard, TrackFacts } from '../../components/TrackCard/TrackCard'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { useNotification } from '../../context/NotificationContext'
import { playWhenAllowed } from '../../playWhenAllowed'
import { plural } from '../../plural'
import { useEnterToContinue } from '../../useEnterToContinue'
import './DiscoverPage.css'

type Answer = {
  knows: boolean
  guess: Game | null
  corrected: boolean
}

type Drag = {
  startX: number
  startY: number
  x: number
  y: number
  over: boolean | null
}

function answerTitle(answer: Answer): string {
  if (answer.corrected) return 'Connue'
  if (answer.guess) return answer.knows ? 'Jeu trouvé !' : `Ce n'était pas ${answer.guess.name}`
  return answer.knows ? 'Connue' : 'Pas connue'
}

export function DiscoverPage() {
  const { listener } = useCurrentListener()
  const { notify } = useNotification()
  const [track, setTrack] = useState<DiscoverTrack | null>(null)
  const [error, setError] = useState(false)
  const [games, setGames] = useState<Game[]>([])
  const [answer, setAnswer] = useState<Answer | null>(null)
  const pendingVote = useRef<Promise<void>>(Promise.resolve())
  const [drag, setDrag] = useState<Drag | null>(null)
  const knownRef = useRef<HTMLButtonElement>(null)
  const unknownRef = useRef<HTMLButtonElement>(null)
  const audioRef = useRef<HTMLAudioElement>(null)
  const trackId = track?.trackId

  function loadNext() {
    if (!listener) return
    const listenerId = listener.id
    setTrack(null)
    setAnswer(null)
    pendingVote.current
      .then(() => fetchNextTrack(listenerId))
      .then(setTrack)
      .catch(() => setError(true))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadNext, [listener])

  useEnterToContinue(answer !== null, loadNext)

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    audio.volume = loadStoredVolume()
    return playWhenAllowed(audio)
  }, [trackId])

  useEffect(() => {
    const controller = new AbortController()
    fetchGames(controller.signal)
      .then(setGames)
      .catch(() => {
        if (!controller.signal.aborted) notify('Impossible de charger la liste des jeux.', 'error')
      })
    return () => controller.abort()
  }, [notify])

  function saveVote(trackId: number, knows: boolean) {
    if (!listener) return
    pendingVote.current = setKnowledge(listener.id, trackId, knows).catch(() =>
      notify("Le vote n'a pas été enregistré : cette musique sera reproposée plus tard.", 'error'),
    )
  }

  function choose(knows: boolean, guess: Game | null) {
    if (track === null || track.trackId === null || answer) return
    saveVote(track.trackId, knows)
    setAnswer({ knows, guess, corrected: false })
  }

  function knowAnyway() {
    if (track === null || track.trackId === null || !answer) return
    saveVote(track.trackId, true)
    setAnswer({ ...answer, knows: true, corrected: true })
  }

  function dropTarget(x: number, y: number): boolean | null {
    const inside = (element: HTMLElement | null) => {
      if (!element) return false
      const rect = element.getBoundingClientRect()
      return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
    }
    if (inside(knownRef.current)) return true
    if (inside(unknownRef.current)) return false
    return null
  }

  function startDrag(event: PointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId)
    setDrag({ startX: event.clientX, startY: event.clientY, x: 0, y: 0, over: null })
  }

  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    if (!drag) return
    setDrag({
      ...drag,
      x: event.clientX - drag.startX,
      y: event.clientY - drag.startY,
      over: dropTarget(event.clientX, event.clientY),
    })
  }

  function endDrag(event: PointerEvent<HTMLDivElement>) {
    if (!drag) return
    const over = dropTarget(event.clientX, event.clientY)
    setDrag(null)
    if (over !== null) choose(over, null)
  }

  if (!listener) {
    return <LoginPrompt message="Il faut un pseudo pour découvrir des musiques." />
  }
  if (error) {
    return <p role="alert">Impossible de charger une musique.</p>
  }

  const stageResult = answer ? (answer.knows ? ' discover-known' : ' discover-unknown') : ''

  return (
    <>
      <h1>Découvrir</h1>

      {track === null ? (
        <p>Chargement...</p>
      ) : track.finished ? (
        <p>Toutes les musiques jouables ont déjà un vote.</p>
      ) : (
        <>
          <p className="progress">
            {plural(track.voted, 'musique votée', 'musiques votées')} &middot;{' '}
            {plural(track.remaining, 'restante', 'restantes')}
          </p>

          <div className={`discover-stage${stageResult}`}>
            <button
              ref={knownRef}
              type="button"
              className={`discover-choice discover-choice-known${drag?.over === true ? ' discover-choice-over' : ''}`}
              onClick={() => choose(true, null)}
              disabled={answer !== null}
            >
              <CheckIcon />
              <span>Je connais</span>
            </button>

            <TrackCard
              result={answer ? (answer.knows ? 'success' : 'failure') : null}
              media={
                <>
                  {answer ? (
                    <GameCover />
                  ) : (
                    <MysteryCover
                      className={`discover-cover-draggable${drag ? ' discover-cover-dragging' : ''}`}
                      style={drag ? { translate: `${drag.x}px ${drag.y}px` } : undefined}
                      onPointerDown={startDrag}
                      onPointerMove={moveDrag}
                      onPointerUp={endDrag}
                      onPointerCancel={() => setDrag(null)}
                    />
                  )}
                  <audio
                    key={track.trackId}
                    ref={audioRef}
                    controls
                    loop
                    src={track.audioLink ?? undefined}
                    onVolumeChange={(event) => storeVolume(event.currentTarget.volume)}
                  />
                </>
              }
              details={
                answer &&
                track.trackId !== null && (
                  <>
                    <TrackFacts
                      success={answer.knows}
                      title={answerTitle(answer)}
                      franchise={track.franchiseName}
                      game={track.gameName}
                      track={track.trackName}
                    />
                    {!answer.knows && (
                      <button type="button" onClick={knowAnyway}>
                        Je connais en fait
                      </button>
                    )}
                    <Link to={`/tracks/${track.trackId}`} target="_blank">
                      Voir la fiche de la musique
                    </Link>
                    <ReportButton trackId={track.trackId} />
                  </>
                )
              }
            />

            <button
              ref={unknownRef}
              type="button"
              className={`discover-choice discover-choice-unknown${drag?.over === false ? ' discover-choice-over' : ''}`}
              onClick={() => choose(false, null)}
              disabled={answer !== null}
            >
              <CrossIcon />
              <span>Je ne connais pas</span>
            </button>
          </div>

          {answer ? (
            <button type="button" className="button-primary track-next" onClick={loadNext} autoFocus>
              Suivant
            </button>
          ) : (
            <div className="track-search">
              <NamePicker
                key={track.trackId}
                items={games}
                selected={null}
                onSelect={(game) => choose(game.id === track.gameId, game)}
                onClear={() => {}}
                placeholder="Deviner le jeu"
                autoFocus
                renderLabel={(item) => `${item.name} (${item.franchiseName})`}
                getSearchText={(item) => `${item.name} ${item.franchiseName}`}
              />
            </div>
          )}
        </>
      )}
    </>
  )
}
