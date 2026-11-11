import { describe, expect, it } from 'vitest'
import { normalizeRoomCode } from './roomCode'

describe('normalizeRoomCode', () => {
  it('met le code en majuscules sans espaces', () => {
    expect(normalizeRoomCode(' abcd ')).toBe('ABCD')
    expect(normalizeRoomCode('AB CD')).toBe('ABCD')
  })

  it('accepte le lien complet de la salle', () => {
    expect(normalizeRoomCode('http://192.168.1.20:4672/rooms/QWZP')).toBe('QWZP')
  })
})
