import { request } from './http'

// Forme du JSON renvoyé par GET /api/tracks/{trackId}/reports/{listenerId} (voir TrackReportStatusResponse côté backend)
export type ReportStatus = {
  reports: number
  reportedByListener: boolean
}

export async function fetchReportStatus(trackId: number, listenerId: number, signal?: AbortSignal): Promise<ReportStatus> {
  const response = await request(`/api/tracks/${trackId}/reports/${listenerId}`, { signal })
  return response.json()
}

export async function reportTrack(trackId: number, listenerId: number): Promise<void> {
  await request(`/api/tracks/${trackId}/reports/${listenerId}`, { method: 'PUT' })
}

export async function cancelReport(trackId: number, listenerId: number): Promise<void> {
  await request(`/api/tracks/${trackId}/reports/${listenerId}`, { method: 'DELETE' })
}
