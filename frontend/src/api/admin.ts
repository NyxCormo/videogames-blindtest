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

export type AdminKind = 'franchises' | 'games' | 'tracks' | 'blindtests' | 'listeners' | 'tags'

export async function renameItem(token: string, kind: AdminKind, id: number, name: string): Promise<{ id: number; name: string }> {
  const response = await adminRequest(token, `/api/admin/${kind}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  return response.json()
}

export async function mergeItem(token: string, kind: AdminKind, id: number, targetId: number): Promise<void> {
  await adminRequest(token, `/api/admin/${kind}/${id}/merge`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetId }),
  })
}

export async function deleteItem(token: string, kind: AdminKind, id: number): Promise<void> {
  await adminRequest(token, `/api/admin/${kind}/${id}`, { method: 'DELETE' })
}

export type ListenerUsage = {
  id: number
  name: string
  votes: number
  blindtests: number
}

export async function fetchListenerUsage(token: string): Promise<ListenerUsage[]> {
  const response = await adminRequest(token, '/api/admin/listeners')
  return response.json()
}

export type RefreshStatus = {
  running: boolean
  startedAt: string | null
  finishedAt: string | null
  lastReport: { checked: number; alive: number; refreshed: number; failed: number } | null
}

export async function fetchRefreshStatus(token: string): Promise<RefreshStatus> {
  const response = await adminRequest(token, '/api/admin/audio-links/refresh')
  return response.json()
}

export async function startRefresh(token: string): Promise<RefreshStatus> {
  const response = await adminRequest(token, '/api/admin/audio-links/refresh', { method: 'POST' })
  return response.json()
}

export type TagCount = {
  id: number
  name: string
  typeName: string
  tracks: number
}

export async function fetchTagCounts(token: string): Promise<TagCount[]> {
  const response = await adminRequest(token, '/api/admin/tags')
  return response.json()
}

export type DeletePreview = {
  allowed: boolean
  impact: string
}

export async function fetchDeletePreview(token: string, kind: AdminKind, id: number): Promise<DeletePreview> {
  const response = await adminRequest(token, `/api/admin/${kind}/${id}/delete-preview`)
  return response.json()
}
