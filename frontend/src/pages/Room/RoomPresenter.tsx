import { useState } from 'react'
import { nextInRoom, revealInRoom, type CurrentTrack, type Room } from '../../api/rooms'
import { CheckIcon } from '../../components/icons'
import { GameCover, MysteryCover, TrackCard, TrackFacts } from '../../components/TrackCard/TrackCard'
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

export function RoomPresenter({ room, track, listenerId, offset }: Props) {
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const startsAt = Date.parse(track.startsAt)
  const endsAt = Date.parse(track.endsAt)
  const started = useServerReached(startsAt, offset)
  const ended = useServerReached(endsAt, offset)
  const reveal = track.reveal
  const presentPlayers = room.players.filter((player) => player.present)
  const doneCount = presentPlayers.filter((player) => player.done).length
  const isLast = !room.infinite && track.position + 1 >= room.trackCount

  function send(action: (code: string, listenerId: number) => Promise<Room>) {
    setSending(true)
    setError(null)
    action(room.code, listenerId)
      .catch((err: Error) => setError(err.message))
      .finally(() => setSending(false))
  }

  return (
    <section className="room-game">
      <div className="room-game-bar">
        <p className="progress">
          {trackProgress(room, track.position)}
        </p>
        {track.audioLink && <RoomAudio src={track.audioLink} startsAt={startsAt} offset={offset} free />}
      </div>
      {!track.audioLink && <p role="alert">Pas de lien audio pour cette musique.</p>}

      {reveal ? (
        <TrackCard
          result={null}
          media={<GameCover gameId={reveal.gameId} gameName={reveal.gameName} coverUrl={reveal.coverUrl} />}
          details={
            <TrackFacts
              success
              title="Réponse"
              franchise={reveal.franchiseName}
              game={reveal.gameName}
              track={reveal.trackName}
            />
          }
        />
      ) : (
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
              <div className="room-done" role="status">
                {ended && <p>Temps de réponse écoulé.</p>}
                {presentPlayers.length === 0 ? (
                  <p>Aucun joueur présent.</p>
                ) : doneCount === presentPlayers.length ? (
                  <p>
                    <CheckIcon /> Tout le monde a répondu
                  </p>
                ) : (
                  <p>
                    Joueurs présents ayant fini : {doneCount} sur {presentPlayers.length}
                  </p>
                )}
              </div>
            </>
          }
        />
      )}

      {reveal ? (
        <button type="button" className="button-primary track-next" onClick={() => send(nextInRoom)} disabled={sending}>
          {isLast ? 'Afficher le classement final' : 'Musique suivante'}
        </button>
      ) : (
        <button type="button" className="button-primary track-next" onClick={() => send(revealInRoom)} disabled={sending}>
          Révéler
        </button>
      )}
      {error && <p role="alert">{error}</p>}
    </section>
  )
}
