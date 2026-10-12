// Forme du JSON renvoyé par GET /api/tracks/{trackId}/reports/{listenerId} (voir TrackReportStatusResponse côté backend)
export type ReportStatus = {
  reports: number
  reportedByListener: boolean
}

export async function fetchReportStatus(trackId: number, listenerId: number, signal?: AbortSignal): Promise<ReportStatus> {
  const response = await fetch(`/api/tracks/${trackId}/reports/${listenerId}`, { signal })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
  return response.json()
}

export async function reportTrack(trackId: number, listenerId: number): Promise<void> {
  const response = await fetch(`/api/tracks/${trackId}/reports/${listenerId}`, { method: 'PUT' })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}

export async function cancelReport(trackId: number, listenerId: number): Promise<void> {
  const response = await fetch(`/api/tracks/${trackId}/reports/${listenerId}`, { method: 'DELETE' })
  if (!response.ok) {
    throw new Error(`Erreur ${response.status}`)
  }
}
