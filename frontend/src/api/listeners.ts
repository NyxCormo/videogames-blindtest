import { request } from './http'

// Forme du JSON renvoyé par GET/POST /api/listeners (voir ListenerResponse côté backend)
export type Listener = {
  id: number
  name: string
}

export async function searchListeners(search: string, signal?: AbortSignal): Promise<Listener[]> {
  const response = await request(`/api/listeners?search=${encodeURIComponent(search)}`, { signal })
  return response.json()
}

export async function createListener(name: string): Promise<Listener> {
  const response = await request('/api/listeners', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return response.json()
}
