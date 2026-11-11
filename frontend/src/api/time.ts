import { request } from './http'

export async function fetchServerTime(): Promise<number> {
  const response = await request('/api/time')
  const body: { now: number } = await response.json()
  return body.now
}
