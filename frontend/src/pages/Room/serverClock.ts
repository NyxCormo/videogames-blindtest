export type ClockSample = {
  sentAt: number
  serverTime: number
  receivedAt: number
}

export function clockOffset(samples: ClockSample[]): number {
  const fastest = samples.reduce((best, sample) =>
    sample.receivedAt - sample.sentAt < best.receivedAt - best.sentAt ? sample : best,
  )
  return fastest.serverTime - (fastest.sentAt + fastest.receivedAt) / 2
}
