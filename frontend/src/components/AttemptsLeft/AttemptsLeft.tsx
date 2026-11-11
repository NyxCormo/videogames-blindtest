import { plural } from '../../plural'
import { CrossIcon } from '../icons'
import './AttemptsLeft.css'

type Props = {
  remaining: number
  max: number
  misses: string[]
}

export function AttemptsLeft({ remaining, max, misses }: Props) {
  return (
    <>
      <p className="attempts">
        <span className="attempts-dots" aria-hidden="true">
          {Array.from({ length: max }, (_, index) => (
            <span key={index} className={`attempts-dot${index < remaining ? ' attempts-dot-left' : ''}`} />
          ))}
        </span>
        {plural(remaining, 'essai restant', 'essais restants')}
      </p>
      {misses.length > 0 && (
        <ul className="attempts-misses" role="alert">
          {misses.map((miss, index) => (
            <li key={index}>
              <CrossIcon /> {miss}
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
