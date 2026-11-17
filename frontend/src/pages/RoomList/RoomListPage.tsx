import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { fetchPublicRooms, type Room } from '../../api/rooms'
import { plural } from '../../plural'
import { PHASE_LABELS } from '../Room/roomLabels'
import { normalizeRoomCode, ROOM_CODE_LENGTH } from './roomCode'
import './RoomListPage.css'

const REFRESH_MS = 10_000

export function RoomListPage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [rooms, setRooms] = useState<Room[] | null>(null)
  const [error, setError] = useState(false)
  const normalizedCode = normalizeRoomCode(code)

  useEffect(() => {
    const controller = new AbortController()
    function load() {
      fetchPublicRooms(controller.signal)
        .then((loaded) => {
          setRooms(loaded)
          setError(false)
        })
        .catch(() => {
          if (!controller.signal.aborted) setError(true)
        })
    }
    load()
    const interval = setInterval(load, REFRESH_MS)
    return () => {
      clearInterval(interval)
      controller.abort()
    }
  }, [])

  function handleJoin(event: FormEvent) {
    event.preventDefault()
    navigate(`/rooms/${normalizedCode}`)
  }

  return (
    <>
      <h1>Salles</h1>
      <p>
        Une salle permet de jouer un blindtest à plusieurs, en même temps. <Link to="/rooms/new">Créer une salle</Link>
      </p>

      <form onSubmit={handleJoin} className="room-join">
        <label>
          Code de la salle
          <input
            type="text"
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="ABCD"
            autoCapitalize="characters"
            autoComplete="off"
          />
        </label>
        <button type="submit" className="button button-primary" disabled={normalizedCode.length !== ROOM_CODE_LENGTH}>
          Rejoindre
        </button>
      </form>

      <h2>Salles publiques</h2>
      {error && <p role="alert">Impossible de charger les salles. Le backend est-il lancé ?</p>}
      {!error && rooms === null && <p>Chargement...</p>}
      {rooms !== null && rooms.length === 0 && <p>Aucune salle publique pour l'instant.</p>}
      {rooms !== null && rooms.length > 0 && (
        <ul className="room-list">
          {rooms.map((room) => (
            <li key={room.code}>
              <span className="room-list-code">{room.code}</span>
              <span className="room-list-name">{room.blindtestName}</span>
              <span className="room-list-info">
                {PHASE_LABELS[room.phase]} · {plural(room.players.length, 'joueur', 'joueurs')}
              </span>
              <Link to={`/rooms/${room.code}`}>Rejoindre</Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
