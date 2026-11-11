import type { Room } from '../../api/rooms'

export function latestRoom(current: Room | null, next: Room): Room {
  if (current !== null && current.code === next.code && current.version > next.version) {
    return current
  }
  return next
}
