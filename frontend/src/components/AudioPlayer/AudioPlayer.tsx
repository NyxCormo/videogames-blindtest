import { forwardRef, useState } from 'react'
import type { Track } from '../../api/tracks'
import { TrackLabel } from '../TrackLabel/TrackLabel'
import { NextIcon, PreviousIcon, RepeatIcon } from '../icons'
import { ReportButton } from '../ReportButton/ReportButton'
import './AudioPlayer.css'


type Props = {
    track: Track | null
    onPlayingChange: (playing: boolean) => void
    onNext: () => void
    onPrevious: () => void
    canGoBack: boolean
}

// L'élément <audio> reste toujours monté (même sans musique sélectionnée) pour que la page
// puisse y accéder par ref dès le premier clic sur un bouton de lecture
export const AudioPlayer = forwardRef<HTMLAudioElement, Props>(function AudioPlayer(
  { track, onPlayingChange, onNext, onPrevious, canGoBack },
  ref,
) {
  const [loop, setLoop] = useState(false)

return (
    <div className="audio-player" hidden={!track}>
      {track && (
        <span className="audio-player-title">
          <TrackLabel franchise={track.franchiseName} game={track.gameName} track={track.name} />
        </span>
      )}
      <audio
        ref={ref}
        controls
        loop={loop}
        onPlay={() => onPlayingChange(true)}
        onPause={() => onPlayingChange(false)}
        onEnded={() => {
          onPlayingChange(false)
          onNext()
        }}
      />
      <button type="button" aria-label="Musique précédente" disabled={!canGoBack} onClick={onPrevious}>
        <PreviousIcon />
      </button>
      <button type="button" aria-label="Musique suivante" onClick={onNext}>
        <NextIcon />
      </button>
      <button
        type="button"
        className="audio-player-loop"
        aria-label="Répéter cette musique"
        aria-pressed={loop}
        onClick={() => setLoop(!loop)}
      >
        <RepeatIcon />
      </button>
      {track && <ReportButton trackId={track.id} />}
    </div>
  )
})
