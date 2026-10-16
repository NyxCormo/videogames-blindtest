import { memo, useCallback, useEffect, useRef } from 'react'
import { Link } from 'react-router'
import { hasSource, type Track } from '../../api/tracks'
import { useProgressiveList } from '../../useProgressiveList'
import { PauseIcon, PlayIcon } from '../icons'
import './TrackTable.css'

type Props = {
  tracks: Track[]
  onPlay: (track: Track) => void
  playingTrackId?: number
  resetKey?: string
}

export function TrackTable({ tracks, onPlay, playingTrackId, resetKey = '' }: Props) {
  const shown = useProgressiveList(tracks, resetKey)
  const onPlayRef = useRef(onPlay)
  useEffect(() => {
    onPlayRef.current = onPlay
  })
  const play = useCallback((track: Track) => onPlayRef.current(track), [])

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
          {shown.map((track) => (
            <TrackRow key={track.id} track={track} playing={track.id === playingTrackId} onPlay={play} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

type TrackRowProps = {
  track: Track
  playing: boolean
  onPlay: (track: Track) => void
}

const TrackRow = memo(function TrackRow({ track, playing, onPlay }: TrackRowProps) {
  return (
    <tr>
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
          <button
            type="button"
            className="track-table-play"
            aria-label={`Écouter ${track.name}`}
            aria-pressed={playing}
            onClick={() => onPlay(track)}
          >
            {playing ? <PauseIcon /> : <PlayIcon />}
          </button>
        )}
      </td>
    </tr>
  )
})

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
