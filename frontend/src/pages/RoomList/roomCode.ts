export const ROOM_CODE_LENGTH = 4

export function normalizeRoomCode(input: string): string {
  const lastPart = input.trim().split('/').pop() ?? ''
  return lastPart.toUpperCase().replace(/[^A-Z]/g, '')
}
