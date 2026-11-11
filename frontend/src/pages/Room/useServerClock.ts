import { useEffect, useState } from 'react'
import { fetchServerTime } from '../../api/time'
import { clockOffset, type ClockSample } from './serverClock'

const SAMPLES = 3

export function useClockOffset(): number {
  const [offset, setOffset] = useState(0)

  useEffect(() => {
    let cancelled = false
    async function measure() {
      const samples: ClockSample[] = []
      for (let i = 0; i < SAMPLES; i++) {
        const sentAt = Date.now()
        const serverTime = await fetchServerTime()
        samples.push({ sentAt, serverTime, receivedAt: Date.now() })
      }
      if (!cancelled) setOffset(clockOffset(samples))
    }
    measure().catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return offset
}

export function useServerNow(offset: number): number {
  const [now, setNow] = useState(() => Date.now() + offset)

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now() + offset), 250)
    return () => clearInterval(timer)
  }, [offset])

  return now
}

export function useServerReached(at: number, offset: number): boolean {
  const [reachedAt, setReachedAt] = useState<number | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => setReachedAt(at), at - (Date.now() + offset))
    return () => clearTimeout(timer)
  }, [at, offset])

  return reachedAt === at
}
