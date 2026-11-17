import { useEffect, useState } from 'react'
import type { Room } from '../../api/rooms'
import { latestRoom } from './latestRoom'

export type RoomConnection = 'connecting' | 'open' | 'closed'

export function useRoomEvents(code: string | null) {
  const [room, setRoom] = useState<Room | null>(null)
  const [connection, setConnection] = useState<RoomConnection>('connecting')

  useEffect(() => {
    if (code === null) return
    const source = new EventSource(`/api/rooms/${code}/events`)
    source.addEventListener('room', (event) => {
      setRoom((current) => latestRoom(current, JSON.parse(event.data)))
      setConnection('open')
    })
    source.onerror = () => {
      setConnection(source.readyState === EventSource.CLOSED ? 'closed' : 'connecting')
    }
    return () => source.close()
  }, [code])

  return { room, connection }
}
