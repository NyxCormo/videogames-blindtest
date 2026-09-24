import { Link } from 'react-router'
import { hasSource, type Track } from '../../api/tracks'
import './TrackTable.css'

type Props = {
  tracks: Track[]
  onPlay: (track: Track) => void
  playingTrackId?: number
}

export function TrackTable({ tracks, onPlay, playingTrackId }: Props) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Franchise</th>
            <th>Jeu</th>
            <th>Musique</th>
            <th>Source</th>
            <th>Écouter</th>
          </tr>
        </thead>
        <tbody>
          {tracks.map((track) => (
            <tr key={track.id}>
              <td>{track.franchiseName}</td>
              <td>{track.gameName}</td>
              <td>
                <Link to={`/tracks/${track.id}`}>{track.name}</Link>
              </td>
              <td>
                <Sources track={track} />
              </td>
              <td>
                {track.audioLink && (
                  <button type="button" aria-label={`Écouter ${track.name}`} onClick={() => onPlay(track)}>
                    {track.id === playingTrackId ? '⏸' : '▶'}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Sources({ track }: { track: Track }) {
  if (!hasSource(track)) {
    return <span className="none">Aucune</span>
  }
  return (
    <>
      {track.khinsiderLink && (
        <a className="badge" href={track.khinsiderLink} target="_blank" rel="noreferrer">
          KHInsider
        </a>
      )}
      {track.youtubeLink && (
        <a className="badge" href={track.youtubeLink} target="_blank" rel="noreferrer">
          YouTube
        </a>
      )}
    </>
  )
}
