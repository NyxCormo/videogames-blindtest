import { forwardRef } from 'react'
import type { Track } from '../../api/tracks'
import './AudioPlayer.css'


type Props = {
    track: Track | null
    onPlayingChange: (playing: boolean) => void
    onNext: () => void
}

// L'élément <audio> reste toujours monté (même sans musique sélectionnée) pour que la page
// puisse y accéder par ref dès le premier clic sur un bouton de lecture
export const AudioPlayer = forwardRef<HTMLAudioElement, Props>(function AudioPlayer(
  { track, onPlayingChange, onNext },
  ref,
) {
return (
    <div className="audio-player" hidden={!track}>
      {track && (
        <span className="audio-player-title">
          {track.franchiseName} — {track.gameName} — {track.name}
        </span>
      )}
      <audio
        ref={ref}
        controls
        onPlay={() => onPlayingChange(true)}
        onPause={() => onPlayingChange(false)}
        onEnded={() => onPlayingChange(false)}
      />
      <button type="button" aria-label="Musique suivante" onClick={onNext}>
        ⏭
      </button>
    </div>
  )
})
