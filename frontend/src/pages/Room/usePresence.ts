import { useEffect } from 'react'
import { leaveRoom, sendPresence } from '../../api/rooms'

const HEARTBEAT_MS = 10_000

export function usePresence(code: string, listenerId: number | undefined, active: boolean) {
  useEffect(() => {
    if (!active || listenerId === undefined) return
    const id = listenerId

    function beat() {
      sendPresence(code, id).catch(() => {})
    }

    function leave() {
      leaveRoom(code, id)
    }

    function handleVisibility() {
      if (document.visibilityState === 'visible') beat()
    }

    beat()
    const timer = setInterval(beat, HEARTBEAT_MS)
    window.addEventListener('pagehide', leave)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => {
      clearInterval(timer)
      window.removeEventListener('pagehide', leave)
      document.removeEventListener('visibilitychange', handleVisibility)
      leave()
    }
  }, [code, listenerId, active])
}
