import type { HTMLAttributes, ReactNode } from 'react'
import { CheckIcon, CrossIcon, NoImageIcon } from '../icons'
import './TrackCard.css'

type CardProps = {
  result: 'success' | 'failure' | null
  media: ReactNode
  details?: ReactNode
}

export function TrackCard({ result, media, details }: CardProps) {
  return (
    <div className={`track-card${result ? ` track-card-${result}` : ''}`}>
      <div className="track-media">{media}</div>
      {details && <div className="track-details">{details}</div>}
    </div>
  )
}

export function MysteryCover({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div {...rest} className={`track-cover${className ? ` ${className}` : ''}`} role="img" aria-label="Jeu à deviner">
      ?
    </div>
  )
}

export function GameCover() {
  return (
    <>
      <div className="track-cover" role="img" aria-label="Pas encore d'image pour ce jeu">
        <NoImageIcon />
      </div>
      <button type="button" className="track-wrong-image" disabled>
        Ce n'est pas la bonne image
      </button>
    </>
  )
}

type FactsProps = {
  success: boolean
  title: string
  franchise: string | null
  game: string | null
  track: string | null
}

export function TrackFacts({ success, title, franchise, game, track }: FactsProps) {
  return (
    <>
      <p className="track-result">
        {success ? <CheckIcon /> : <CrossIcon />}
        {title}
      </p>
      <dl className="track-facts">
        <dt>Franchise</dt>
        <dd>{franchise}</dd>
        <dt>Jeu</dt>
        <dd>{game}</dd>
        <dt>Musique</dt>
        <dd>{track}</dd>
      </dl>
    </>
  )
}
