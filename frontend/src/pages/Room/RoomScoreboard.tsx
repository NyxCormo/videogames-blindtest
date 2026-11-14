import type { Room } from '../../api/rooms'
import { CheckIcon } from '../../components/icons'
import { plural } from '../../plural'
import { ranking } from './ranking'

type Props = {
  room: Room
  listenerId: number
}

export function RoomScoreboard({ room, listenerId }: Props) {
  return (
    <ol className="room-scoreboard">
      {ranking(room.players).map((player) => (
        <li key={player.listenerId} className={player.listenerId === listenerId ? 'room-scoreboard-me' : undefined}>
          <span className="room-scoreboard-rank">{player.rank}</span>
          <span className="room-scoreboard-name">
            {player.name}
            <span className="room-scoreboard-heard">
              {plural(player.tracksHeard, 'musique écoutée', 'musiques écoutées')}
            </span>
          </span>
          {!player.present && <span className="room-absent">absent</span>}
          {room.phase === 'PLAYING' && player.done && (
            <span className="room-scoreboard-done">
              <CheckIcon /> fini
            </span>
          )}
          {player.roundPoints !== null && <span className="room-scoreboard-round">+{player.roundPoints}</span>}
          <span className="room-scoreboard-score">{player.score} pts</span>
        </li>
      ))}
    </ol>
  )
}
