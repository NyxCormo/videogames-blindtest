import { hasSource, type Track } from '../../api/tracks'
import './TrackTable.css'

export function TrackTable({ tracks }: { tracks: Track[] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Franchise</th>
            <th>Jeu</th>
            <th>Musique</th>
            <th>Source</th>
          </tr>
        </thead>
        <tbody>
          {tracks.map((track) => (
            <tr key={track.id}>
              <td>{track.franchiseName}</td>
              <td>{track.gameName}</td>
              <td>{track.name}</td>
              <td>
                <Sources track={track} />
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
