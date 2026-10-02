import type { Track } from './tracks'

const ADMIN_USERNAME = 'admin'

export class AdminSessionExpired extends Error {
  constructor() {
    super('Session admin expirée : il faut se reconnecter.')
  }
}

export type LinkKind = 'khinsider' | 'youtube'

export async function loginAdmin(password: string): Promise<string> {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ADMIN_USERNAME, password }),
  })
  if (!response.ok) {
    const body = await response.json()
    throw new Error(body.message ?? `Erreur ${response.status}`)
  }
  const body: { token: string } = await response.json()
  return body.token
}

export async function logoutAdmin(token: string): Promise<void> {
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

export async function checkAdmin(token: string, signal?: AbortSignal): Promise<boolean> {
  const response = await fetch('/api/admin/check', {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })
  if (response.status === 401) {
    return false
  }
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return true
}

async function adminRequest(token: string, url: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(url, { ...init, headers: { ...init.headers, Authorization: `Bearer ${token}` } })
  if (response.status === 401) {
    throw new AdminSessionExpired()
  }
  if (!response.ok) {
    const body = await response.json()
    throw new Error(body.message ?? `Erreur ${response.status}`)
  }
  return response
}

export async function replaceTrackLink(token: string, trackId: number, kind: LinkKind, link: string): Promise<Track> {
  const response = await adminRequest(token, `/api/admin/tracks/${trackId}/${kind}-link`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ link }),
  })
  return response.json()
}

export async function removeTrackLink(token: string, trackId: number, kind: LinkKind): Promise<Track> {
  const response = await adminRequest(token, `/api/admin/tracks/${trackId}/${kind}-link`, { method: 'DELETE' })
  return response.json()
}

export type RenameKind = 'franchises' | 'games' | 'tracks'

export async function renameItem(token: string, kind: RenameKind, id: number, name: string): Promise<{ id: number; name: string }> {
  const response = await adminRequest(token, `/api/admin/${kind}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return response.json()
}
