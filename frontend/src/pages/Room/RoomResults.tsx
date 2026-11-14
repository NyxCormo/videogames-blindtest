import { Link } from 'react-router'
import type { Room } from '../../api/rooms'
import { CoverThumbnail } from '../../components/CoverThumbnail/CoverThumbnail'
import { plural } from '../../plural'
import { rankLabel, ranking } from './ranking'

type Props = {
  room: Room
  listenerId: number
  closed: boolean
}

export function RoomResults({ room, listenerId, closed }: Props) {
  const ranked = ranking(room.players)
  const me = ranked.find((player) => player.listenerId === listenerId)
  const played = room.playedTracks.length

  return (
    <section className="room-results">
      <h2>Partie terminée</h2>
      <p className="muted">
        {played === 0
          ? 'Partie arrêtée avant la fin de la première musique.'
          : played < room.trackCount
            ? `Partie arrêtée après ${plural(played, 'musique', 'musiques')} sur ${room.trackCount}.`
            : `${plural(played, 'musique jouée', 'musiques jouées')}.`}
        {closed && ' La salle est fermée.'}
      </p>

      {ranked.length === 0 ? (
        <p>Aucun joueur n'a participé.</p>
      ) : (
        <>
          <ol className="room-podium">
            {ranked.slice(0, 3).map((player) => (
              <li key={player.listenerId} className={`room-podium-step room-podium-${player.rank}`}>
                <span className="room-podium-name">{player.name}</span>
                <span className="room-podium-score">{player.score} pts</span>
                <span className="room-podium-block">{rankLabel(player.rank)}</span>
              </li>
            ))}
          </ol>
          {me && (
            <p className="room-results-me">
              Place : {rankLabel(me.rank)} sur {ranked.length}, {plural(me.score, 'point', 'points')}
            </p>
          )}

          <div className="table-wrap">
            <table className="room-results-table">
              <thead>
                <tr>
                  <th>Place</th>
                  <th>Joueur</th>
                  <th>Points</th>
                  <th>Jeux</th>
                  <th>Franchises seules</th>
                  <th>Bonus</th>
                </tr>
              </thead>
              <tbody>
                {ranked.map((player) => (
                  <tr key={player.listenerId} className={player.listenerId === listenerId ? 'room-results-me-row' : undefined}>
                    <td>{rankLabel(player.rank)}</td>
                    <td>{player.name}</td>
                    <td>{player.score}</td>
                    <td>{player.gamesFound}</td>
                    <td>{player.franchisesFound}</td>
                    <td>{player.bonusesFound}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {played > 0 && (
        <>
          <h3>Musiques de la partie</h3>
          <ol className="room-played">
            {room.playedTracks.map((track, index) => (
              <li key={index}>
                {track.coverUrl ? (
                  <CoverThumbnail url={track.coverUrl} alt="" />
                ) : (
                  <span className="room-played-nocover" aria-hidden="true" />
                )}
                <span className="room-played-text">
                  <Link to={`/tracks/${track.trackId}`}>{track.trackName}</Link>
                  <span className="muted">
                    {track.gameName}
                    {track.franchiseName !== track.gameName && ` (${track.franchiseName})`}
                  </span>
                </span>
              </li>
            ))}
          </ol>
        </>
      )}

      <div className="room-results-actions">
        <Link className="button button-primary" to={`/rooms/new?blindtest=${room.blindtestId}`}>
          Rejouer ce blindtest
        </Link>
        <Link className="button" to="/rooms">
          Retour aux salles
        </Link>
      </div>
    </section>
  )
}
