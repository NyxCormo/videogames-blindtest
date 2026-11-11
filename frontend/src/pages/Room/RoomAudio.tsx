import { useEffect, useRef, useState } from 'react'
import { loadStoredVolume, storeVolume } from '../../audioPreferences'

type Props = {
  src: string
  startsAt: number
  offset: number
}

function playInSync(audio: HTMLAudioElement, startsAt: number, offset: number): Promise<void> {
  let elapsed = Math.max(0, (Date.now() + offset - startsAt) / 1000)
  if (Number.isFinite(audio.duration) && audio.duration > 0) {
    elapsed %= audio.duration
  }
  audio.currentTime = elapsed
  return audio.play()
}

export function RoomAudio({ src, startsAt, offset }: Props) {
  const audioRef = useRef<HTMLAudioElement>(null)
  const [volume, setVolume] = useState(loadStoredVolume)
  const [blocked, setBlocked] = useState(false)

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    const timer = setTimeout(() => {
      playInSync(audio, startsAt, offset).catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'NotAllowedError') setBlocked(true)
      })
    }, startsAt - (Date.now() + offset))
    return () => {
      clearTimeout(timer)
      audio.pause()
    }
  }, [src, startsAt, offset])

  useEffect(() => {
    if (audioRef.current) audioRef.current.volume = volume
  }, [volume])

  function handleUnblock() {
    const audio = audioRef.current
    if (!audio) return
    setBlocked(false)
    playInSync(audio, startsAt, offset).catch(() => setBlocked(true))
  }

  function handleVolume(value: number) {
    setVolume(value)
    storeVolume(value)
  }

  return (
    <div className="room-audio">
      <audio ref={audioRef} src={src} preload="auto" loop />
      {blocked && (
        <button type="button" className="button-primary" onClick={handleUnblock}>
          Activer le son
        </button>
      )}
      <label>
        Volume
        <input
          type="range"
          min={0}
          max={1}
          step={0.05}
          value={volume}
          onChange={(event) => handleVolume(Number(event.target.value))}
        />
      </label>
    </div>
  )
}
