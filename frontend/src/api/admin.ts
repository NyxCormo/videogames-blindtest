import type { Game } from './games'
import { errorMessage, HttpError, request } from './http'
import type { Track } from './tracks'

const ADMIN_USERNAME = 'admin'

export class AdminSessionExpired extends Error {
  constructor() {
    super('Session admin expirée : il faut se reconnecter.')
  }
}

export type LinkKind = 'khinsider' | 'youtube'

export async function loginAdmin(password: string): Promise<string> {
  const response = await request('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ADMIN_USERNAME, password }),
  })
  const body: { token: string } = await response.json()
  return body.token
}

export async function logoutAdmin(token: string): Promise<void> {
  await request('/api/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
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
    throw new HttpError(response.status, await errorMessage(response))
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
  createdAt: string | null
  lastSeenAt: string | null
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

export type ReportedTrack = {
  track: Track
  reporters: string[]
}

export async function fetchReportedTracks(token: string): Promise<ReportedTrack[]> {
  const response = await adminRequest(token, '/api/admin/reports')
  return response.json()
}

export async function dismissReports(token: string, trackId: number): Promise<void> {
  await adminRequest(token, `/api/admin/reports/${trackId}`, { method: 'DELETE' })
}

export type DeletePreview = {
  allowed: boolean
  impact: string
}

export async function fetchDeletePreview(token: string, kind: AdminKind, id: number): Promise<DeletePreview> {
  const response = await adminRequest(token, `/api/admin/${kind}/${id}/delete-preview`)
  return response.json()
}

export type MergePreview = {
  allowed: boolean
  summary: string
}

export async function fetchMergePreview(token: string, kind: AdminKind, id: number, targetId: number): Promise<MergePreview> {
  const response = await adminRequest(token, `/api/admin/${kind}/${id}/merge-preview?into=${targetId}`)
  return response.json()
}

export type GameCover = {
  coverUrl: string | null
  displayUrl: string | null
  downloaded: boolean
}

export type AlbumImages = {
  albumUrl: string
  tracks: string[]
  images: string[]
  fromPage: boolean
}

export type AlbumOverview = {
  albumUrl: string
  pageUrl: string | null
  name: string
  games: Game[]
  tracks: string[]
  scannedAt: string | null
  pageSavedAt: string | null
  images: string[]
}

export async function fetchAlbums(token: string): Promise<AlbumOverview[]> {
  const response = await adminRequest(token, '/api/admin/albums')
  return response.json()
}

export type GameCoverEntry = {
  gameId: number
  displayUrl: string
}

export async function fetchGameCovers(token: string): Promise<GameCoverEntry[]> {
  const response = await adminRequest(token, '/api/admin/covers')
  return response.json()
}

export async function fetchGameCover(token: string, gameId: number): Promise<GameCover> {
  const response = await adminRequest(token, `/api/admin/games/${gameId}/cover`)
  return response.json()
}

export async function fetchCoverCandidates(token: string, gameId: number, rescan: boolean): Promise<AlbumImages[]> {
  const response = await adminRequest(token, `/api/admin/games/${gameId}/cover/candidates?rescan=${rescan}`)
  return response.json()
}

export async function saveGameCover(token: string, gameId: number, coverUrl: string): Promise<GameCover> {
  const response = await adminRequest(token, `/api/admin/games/${gameId}/cover`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ coverUrl }),
  })
  return response.json()
}

export async function removeGameCover(token: string, gameId: number): Promise<GameCover> {
  const response = await adminRequest(token, `/api/admin/games/${gameId}/cover`, { method: 'DELETE' })
  return response.json()
}

export type CoverProposal = {
  game: Game
  currentUrl: string | null
  imageUrl: string | null
  listenerId: number
  listenerName: string
  createdAt: string
}

export async function fetchCoverProposals(token: string): Promise<CoverProposal[]> {
  const response = await adminRequest(token, '/api/admin/cover-proposals')
  return response.json()
}

export async function acceptCoverProposal(token: string, proposal: CoverProposal): Promise<GameCover> {
  const response = await adminRequest(token, `/api/admin/cover-proposals/${proposal.game.id}/${proposal.listenerId}/accept`, {
    method: 'POST',
  })
  return response.json()
}

export async function rejectCoverProposal(token: string, proposal: CoverProposal): Promise<void> {
  await adminRequest(token, `/api/admin/cover-proposals/${proposal.game.id}/${proposal.listenerId}`, { method: 'DELETE' })
}

export type CoverFillMode = 'NEW' | 'WITHOUT_IMAGE' | 'ALL'

export type CoverFillStatus = {
  running: boolean
  startedAt: string | null
  finishedAt: string | null
  lastReport: { scanned: number; toScan: number; covered: number; downloaded: number; notDownloaded: number } | null
  albums: number
  albumsWithoutImage: number
}

export async function fetchCoverFillStatus(token: string): Promise<CoverFillStatus> {
  const response = await adminRequest(token, '/api/admin/covers/fill')
  return response.json()
}

export async function startCoverFill(token: string, mode: CoverFillMode): Promise<CoverFillStatus> {
  const response = await adminRequest(token, `/api/admin/covers/fill?mode=${mode}`, { method: 'POST' })
  return response.json()
}

export type YoutubeAudioFailure = {
  trackId: number
  trackName: string
  gameName: string
  youtubeLink: string
  error: string
}

export type YoutubeAudioStatus = {
  available: boolean
  running: boolean
  startedAt: string | null
  finishedAt: string | null
  lastReport: { done: number; toDownload: number; downloaded: number; failed: number } | null
  tracks: number
  downloaded: number
  failures: YoutubeAudioFailure[]
}

export async function fetchYoutubeAudioStatus(token: string): Promise<YoutubeAudioStatus> {
  const response = await adminRequest(token, '/api/admin/youtube-audio')
  return response.json()
}

export async function startYoutubeAudioDownload(token: string): Promise<YoutubeAudioStatus> {
  const response = await adminRequest(token, '/api/admin/youtube-audio', { method: 'POST' })
  return response.json()
}
