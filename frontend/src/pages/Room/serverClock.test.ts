import { describe, expect, it } from 'vitest'
import { clockOffset } from './serverClock'

describe('clockOffset', () => {
  it('places the server time in the middle of the round trip', () => {
    expect(clockOffset([{ sentAt: 1000, serverTime: 6050, receivedAt: 1100 }])).toBe(5000)
  })

  it('keeps the fastest round trip', () => {
    const samples = [
      { sentAt: 1000, serverTime: 6400, receivedAt: 1600 },
      { sentAt: 2000, serverTime: 7010, receivedAt: 2020 },
      { sentAt: 3000, serverTime: 8100, receivedAt: 3300 },
    ]
    expect(clockOffset(samples)).toBe(5000)
  })

  it('works when the device is ahead of the server', () => {
    expect(clockOffset([{ sentAt: 9000, serverTime: 4010, receivedAt: 9020 }])).toBe(-5000)
  })
})
