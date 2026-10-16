import { useEffect, useState } from 'react'

const ROWS_PER_FRAME = 100

export function useProgressiveList<T>(items: T[], resetKey: string): T[] {
  const [key, setKey] = useState(resetKey)
  const [count, setCount] = useState(ROWS_PER_FRAME)

  if (key !== resetKey) {
    setKey(resetKey)
    setCount(ROWS_PER_FRAME)
  }

  useEffect(() => {
    if (count >= items.length) return
    const frame = requestAnimationFrame(() => setCount((current) => current + ROWS_PER_FRAME))
    return () => cancelAnimationFrame(frame)
  }, [count, items.length])

  return count >= items.length ? items : items.slice(0, count)
}
