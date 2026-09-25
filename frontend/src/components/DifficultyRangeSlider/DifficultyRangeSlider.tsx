import { useRef, type KeyboardEvent, type PointerEvent } from 'react'
import './DifficultyRangeSlider.css'

type Handle = 'min' | 'max'

type Props = {
  min: number
  max: number
  onChange: (min: number, max: number) => void
}

// Barre de difficulté à deux points déplaçables (glisser ou flèches du clavier), sans dépendance externe :
// deux ronds positionnés en % sur une piste, déplacés en calculant la position du pointeur par rapport à la piste.
export function DifficultyRangeSlider({ min, max, onChange }: Props) {
  const trackRef = useRef<HTMLDivElement>(null)

  function valueFromPointer(clientX: number) {
    const track = trackRef.current
    if (!track) {
      return 0
    }
    const rect = track.getBoundingClientRect()
    const ratio = (clientX - rect.left) / rect.width
    return Math.round(Math.min(1, Math.max(0, ratio)) * 100)
  }

  function startDrag(handle: Handle) {
    return (event: PointerEvent) => {
      event.preventDefault()
      function handleMove(moveEvent: globalThis.PointerEvent) {
        const value = valueFromPointer(moveEvent.clientX)
        if (handle === 'min') {
          onChange(Math.min(value, max), max)
        } else {
          onChange(min, Math.max(value, min))
        }
      }
      function stopDrag() {
        window.removeEventListener('pointermove', handleMove)
        window.removeEventListener('pointerup', stopDrag)
      }
      window.addEventListener('pointermove', handleMove)
      window.addEventListener('pointerup', stopDrag)
    }
  }

  function handleKeyDown(handle: Handle) {
    return (event: KeyboardEvent) => {
      let delta = 0
      if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
        delta = -1
      } else if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
        delta = 1
      } else {
        return
      }
      event.preventDefault()
      if (handle === 'min') {
        onChange(Math.min(Math.max(min + delta, 0), max), max)
      } else {
        onChange(min, Math.min(Math.max(max + delta, min), 100))
      }
    }
  }

  return (
    <div className="difficulty-slider" ref={trackRef}>
      <div className="difficulty-slider-track" />
      <div className="difficulty-slider-fill" style={{ left: `${min}%`, width: `${max - min}%` }} />
      <div
        className="difficulty-slider-handle"
        style={{ left: `${min}%` }}
        role="slider"
        aria-label="Difficulté minimale"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={min}
        tabIndex={0}
        onPointerDown={startDrag('min')}
        onKeyDown={handleKeyDown('min')}
      />
      <div
        className="difficulty-slider-handle"
        style={{ left: `${max}%` }}
        role="slider"
        aria-label="Difficulté maximale"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={max}
        tabIndex={0}
        onPointerDown={startDrag('max')}
        onKeyDown={handleKeyDown('max')}
      />
    </div>
  )
}
