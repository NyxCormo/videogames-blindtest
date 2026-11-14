import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { joinRoom, startRoom, stopRoom } from '../../api/rooms'
import { LoginPrompt } from '../../components/LoginPrompt/LoginPrompt'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { plural } from '../../plural'
import { RoomGame } from './RoomGame'
import { DRAW_LABELS, MODE_LABELS, PLAYBACK_LABELS, SCORING_LABELS, VISIBILITY_LABELS } from './roomLabels'
import { RoomPresenter } from './RoomPresenter'
import { RoomResults } from './RoomResults'
import { RoomScoreboard } from './RoomScoreboard'
import { useRoomEvents } from './useRoomEvents'
import { usePresence } from './usePresence'
import { useClockOffset } from './useServerClock'
import './RoomPage.css'

export function RoomPage() {
  const { code = '' } = useParams()
  const roomCode = code.toUpperCase()
  const { listener } = useCurrentListener()
  const listenerId = listener?.id
  const [joinedCode, setJoinedCode] = useState<string | null>(null)
  const [joinError, setJoinError] = useState<{ code: string; message: string } | null>(null)
  const [starting, setStarting] = useState(false)
  const [startError, setStartError] = useState<string | null>(null)
  const [confirmStop, setConfirmStop] = useState(false)
  const [stopError, setStopError] = useState<string | null>(null)
  const { room, connection } = useRoomEvents(joinedCode === roomCode ? roomCode : null)
  const offset = useClockOffset()
  const inGame =
    room !== null && room.phase !== 'FINISHED' && room.players.some((player) => player.listenerId === listenerId)
  usePresence(roomCode, listenerId, inGame)

  useEffect(() => {
    if (listenerId === undefined) return
    let cancelled = false
    joinRoom(roomCode, listenerId)
      .then(() => {
        if (!cancelled) setJoinedCode(roomCode)
      })
      .catch((err: Error) => {
        if (!cancelled) setJoinError({ code: roomCode, message: err.message })
      })
    return () => {
      cancelled = true
    }
  }, [roomCode, listenerId])

  if (!listener) {
    return <LoginPrompt message="Un pseudo est nécessaire pour rejoindre une salle." />
  }

  const error = joinError?.code === roomCode ? joinError.message : null

  if (error || (connection === 'closed' && room?.phase !== 'FINISHED')) {
    return (
      <>
        <h1>Salle {roomCode}</h1>
        <p role="alert">{error ?? 'Cette salle est fermée.'}</p>
        <Link to="/rooms">Retour aux salles</Link>
      </>
    )
  }

  if (!room) {
    return <p>Connexion à la salle...</p>
  }

  const isHost = room.hostId === listener.id
  const presenting = isHost && room.mode === 'PRESENTATION'

  function handleStart() {
    if (!listener) return
    setStarting(true)
    setStartError(null)
    startRoom(roomCode, listener.id)
      .catch((err: Error) => setStartError(err.message))
      .finally(() => setStarting(false))
  }

  function handleStop() {
    if (!listener) return
    setConfirmStop(false)
    setStopError(null)
    stopRoom(roomCode, listener.id).catch((err: Error) => setStopError(err.message))
  }

  return (
    <div className="room">
      <h1>
        Salle <span className="room-code">{room.code}</span>
      </h1>
      {connection === 'connecting' && (
        <p role="status" className="room-reconnecting">
          Connexion perdue, reconnexion en cours...
        </p>
      )}

      {room.phase === 'WAITING' && (
        <dl className="room-settings">
          {room.infinite && room.drawMode !== null && room.franchiseGap !== null ? (
            <>
              <div>
                <dt>Musiques</dt>
                <dd>Infini, {DRAW_LABELS[room.drawMode].toLowerCase()}</dd>
              </div>
              <div>
                <dt>Délai par franchise</dt>
                <dd>{room.franchiseGap === 0 ? 'Aucun' : plural(room.franchiseGap, 'musique', 'musiques')}</dd>
              </div>
              <div>
                <dt>Essais</dt>
                <dd>{room.maxAttempts}</dd>
              </div>
            </>
          ) : (
            <div>
              <dt>Blindtest</dt>
              <dd>
                {room.blindtestName} ({plural(room.trackCount, 'musique', 'musiques')})
              </dd>
            </div>
          )}
          <div>
            <dt>Temps de réponse</dt>
            <dd>{room.answerSeconds} s</dd>
          </div>
          <div>
            <dt>Mode</dt>
            <dd>
              {MODE_LABELS[room.mode]}
              {room.mode === 'PRESENTATION' && `, animée par ${room.hostName}`}
            </dd>
          </div>
          {room.mode === 'MULTI' && (
            <div>
              <dt>Lecture</dt>
              <dd>{room.playersCanSeek ? PLAYBACK_LABELS.FREE : PLAYBACK_LABELS.SYNCED}</dd>
            </div>
          )}
          <div>
            <dt>Points</dt>
            <dd>{SCORING_LABELS[room.scoringMode]}</dd>
          </div>
          <div>
            <dt>Visibilité</dt>
            <dd>{VISIBILITY_LABELS[room.visibility]}</dd>
          </div>
        </dl>
      )}

      {room.phase === 'WAITING' && (
        <section className="room-waiting">
          <p>
            Pour rejoindre : le code <strong>{room.code}</strong> sur la page <Link to="/rooms">Salles</Link>, ou le
            lien <span className="room-url">{`${window.location.origin}/rooms/${room.code}`}</span>
          </p>
          {isHost ? (
            <button type="button" className="button button-primary button-large" onClick={handleStart} disabled={starting}>
              Lancer la partie
            </button>
          ) : (
            <p>En attente du lancement par {room.hostName}.</p>
          )}
          {startError && <p role="alert">{startError}</p>}
        </section>
      )}
      {room.currentTrack &&
        (presenting ? (
          <RoomPresenter
            key={room.currentTrack.position}
            room={room}
            track={room.currentTrack}
            listenerId={listener.id}
            offset={offset}
          />
        ) : (
          <RoomGame
            key={room.currentTrack.position}
            room={room}
            track={room.currentTrack}
            listenerId={listener.id}
            offset={offset}
          />
        ))}
      {room.phase === 'FINISHED' && (
        <RoomResults room={room} listenerId={listener.id} closed={connection === 'closed'} />
      )}
      {isHost && room.phase !== 'FINISHED' && (
        <div className="room-stop">
          {confirmStop ? (
            <>
              <span>La partie s'arrête pour tout le monde, avec les points déjà révélés.</span>
              <button type="button" onClick={handleStop}>
                Arrêter quand même
              </button>
              <button type="button" onClick={() => setConfirmStop(false)}>
                Annuler
              </button>
            </>
          ) : (
            <button type="button" onClick={() => setConfirmStop(true)}>
              Arrêter la partie
            </button>
          )}
        </div>
      )}
      {stopError && <p role="alert">{stopError}</p>}

      {room.phase === 'WAITING' ? (
        <>
          <h2>Joueurs ({room.players.length})</h2>
          {room.players.length === 0 && <p>Aucun joueur pour l'instant.</p>}
          <ul className="room-players">
            {room.players.map((player) => (
              <li key={player.listenerId}>
                {player.name}
                {player.listenerId === room.hostId && <span className="room-badge">Hôte</span>}
                {!player.present && <span className="room-absent">absent</span>}
              </li>
            ))}
          </ul>
        </>
      ) : (
        room.phase !== 'FINISHED' && (
          <>
            <h2>Classement</h2>
            <RoomScoreboard room={room} listenerId={listener.id} />
          </>
        )
      )}
    </div>
  )
}
