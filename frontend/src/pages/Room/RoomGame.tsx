import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import type { Franchise } from '../../api/franchises'
import type { Game } from '../../api/games'
import {
  fetchRound,
  guessFranchiseInRoom,
  guessInRoom,
  passInRoom,
  type CurrentTrack,
  type Room,
  type RoomRound,
} from '../../api/rooms'
import type { Track } from '../../api/tracks'
import { AttemptsLeft } from '../../components/AttemptsLeft/AttemptsLeft'
import { GameGuessForm } from '../../components/GameGuessForm/GameGuessForm'
import { CheckIcon } from '../../components/icons'
import { GameCover, MysteryCover, TrackCard, TrackFacts } from '../../components/TrackCard/TrackCard'
import { useNotification } from '../../context/NotificationContext'
import { plural } from '../../plural'
import { Countdown } from './Countdown'
import { RoomAudio } from './RoomAudio'
import { trackProgress } from './roomLabels'
import { useServerReached } from './useServerClock'

type Props = {
  room: Room
  track: CurrentTrack
  listenerId: number
  offset: number
}

export function RoomGame({ room, track, listenerId, offset }: Props) {
  const { notify } = useNotification()
  const [round, setRound] = useState<RoomRound | null>(null)
  const [misses, setMisses] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const startsAt = Date.parse(track.startsAt)
  const endsAt = Date.parse(track.endsAt)
  const started = useServerReached(startsAt, offset)
  const ended = useServerReached(endsAt, offset)
  const reveal = track.reveal
  const me = room.players.find((player) => player.listenerId === listenerId)
  const presentPlayers = room.players.filter((player) => player.present)
  const doneCount = presentPlayers.filter((player) => player.done).length
  const answering = started && !ended && round !== null && !round.done

  useEffect(() => {
    const controller = new AbortController()
    fetchRound(room.code, listenerId, controller.signal)
      .then(setRound)
      .catch(() => {
        if (!controller.signal.aborted) notify("L'état de la manche n'a pas pu être chargé.", 'error')
      })
    return () => controller.abort()
  }, [room.code, listenerId, notify])

  function send(answer: Promise<RoomRound>, missFor: (next: RoomRound) => string | null) {
    setSubmitting(true)
    answer
      .then((next) => {
        setRound(next)
        const miss = missFor(next)
        if (miss) setMisses((current) => [...current, miss])
      })
      .catch((err: Error) => notify(err.message, 'error'))
      .finally(() => setSubmitting(false))
  }

  function handleGuess(game: Game, guessedTrack: Track | null) {
    send(guessInRoom(room.code, listenerId, game.id, guessedTrack ? guessedTrack.id : null), (next) =>
      next.gameFound ? null : `Ce n'est pas ${game.name}`,
    )
  }

  function handleGuessFranchise(franchise: Franchise) {
    send(guessFranchiseInRoom(room.code, listenerId, franchise.id), (next) =>
      next.foundFranchiseId !== null ? null : `Ce n'est pas la franchise ${franchise.name}`,
    )
  }

  function handlePass() {
    send(passInRoom(room.code, listenerId), () => null)
  }

  return (
    <section className="room-game">
      <div className="room-game-bar">
        <p className="progress">
          {trackProgress(room, track.position)}
        </p>
        {room.mode === 'MULTI' && track.audioLink && (
          <RoomAudio src={track.audioLink} startsAt={startsAt} offset={offset} free={room.playersCanSeek} />
        )}
      </div>
      {room.mode === 'PRESENTATION' && <p className="muted">Le son passe sur l'écran de {room.hostName}.</p>}
      {room.mode === 'MULTI' && !track.audioLink && <p role="alert">Pas de lien audio pour cette musique.</p>}

      {reveal ? (
        <>
          <TrackCard
            result={round?.gameFound ? 'success' : 'failure'}
            media={<GameCover gameId={reveal.gameId} gameName={reveal.gameName} coverUrl={reveal.coverUrl} />}
            details={
              <>
                <TrackFacts
                  success={round?.gameFound ?? false}
                  title={round?.gameFound ? 'Bien joué !' : 'Musique révélée'}
                  franchise={reveal.franchiseName}
                  game={reveal.gameName}
                  track={reveal.trackName}
                />
                {round?.bonusFound && <p className="room-bonus">+ bonus musique trouvée !</p>}
                {me && (
                  <p className="room-round-points">{plural(me.roundPoints ?? 0, 'point', 'points')} sur cette musique</p>
                )}
                <Link to={`/tracks/${reveal.trackId}`} target="_blank">
                  Voir la fiche de la musique
                </Link>
              </>
            }
          />
          <p className="room-next">
            {room.mode === 'PRESENTATION'
              ? `En attente de ${room.hostName}.`
              : room.infinite || track.position + 1 < room.trackCount
                ? 'Musique suivante dans quelques secondes.'
                : 'Fin de la partie dans quelques secondes.'}
          </p>
        </>
      ) : (
        <>
          <TrackCard
            result={null}
            media={
              <>
                <MysteryCover />
                {started ? (
                  <Countdown until={endsAt} offset={offset} total={endsAt - startsAt} />
                ) : (
                  <p className="room-starting">
                    La musique commence dans <Countdown until={startsAt} offset={offset} />
                  </p>
                )}
                {started && round && !round.gameFound && (
                  <AttemptsLeft remaining={round.attemptsRemaining} max={room.maxAttempts} misses={misses} />
                )}
                {answering && (
                  <button type="button" onClick={handlePass} disabled={submitting}>
                    Je passe
                  </button>
                )}
                {ended && !round?.done && <p className="room-ended">Temps de réponse écoulé.</p>}
                {round?.done && (
                  <div className="room-done" role="status">
                    {round.gameFound ? (
                      <p>
                        <CheckIcon /> Jeu trouvé : {plural(round.roundPoints, 'point', 'points')}
                      </p>
                    ) : (
                      <p>Plus de réponse possible sur cette musique.</p>
                    )}
                    <p>
                      Joueurs présents ayant fini : {doneCount} sur {presentPlayers.length}
                    </p>
                  </div>
                )}
              </>
            }
          />
          {answering && (
            <div className="track-search">
              <GameGuessForm
                foundFranchiseId={round.foundFranchiseId}
                onSubmit={handleGuess}
                onGuessFranchise={handleGuessFranchise}
                disabled={submitting}
              />
            </div>
          )}
        </>
      )}
    </section>
  )
}
