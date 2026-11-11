import { useServerNow } from './useServerClock'

type Props = {
  until: number
  offset: number
  total?: number
}

export function Countdown({ until, offset, total }: Props) {
  const now = useServerNow(offset)
  const left = Math.max(0, until - now)
  const seconds = Math.ceil(left / 1000)

  return (
    <span className="room-countdown">
      {total !== undefined && (
        <span className="room-countdown-bar" aria-hidden="true">
          <span style={{ width: `${Math.min(100, (left / total) * 100)}%` }} />
        </span>
      )}
      <span className="room-countdown-seconds">{seconds} s</span>
    </span>
  )
}
