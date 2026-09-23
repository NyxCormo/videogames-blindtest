// Forme du JSON renvoyé par GET/POST /api/listeners (voir ListenerResponse côté backend)
export type Listener = {
  id: number
  name: string
}

export async function searchListeners(search: string, signal?: AbortSignal): Promise<Listener[]> {
  const response = await fetch(`/api/listeners?search=${encodeURIComponent(search)}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function createListener(name: string): Promise<Listener> {
  const response = await fetch('/api/listeners', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!response.ok) {
    const body = await response.json()
    throw new Error(body.message ?? `Erreur ${response.status}`)
  }
  return response.json()
}
