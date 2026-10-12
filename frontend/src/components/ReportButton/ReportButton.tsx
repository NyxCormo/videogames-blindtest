import { useEffect, useState } from 'react'
import { cancelReport, fetchReportStatus, reportTrack, type ReportStatus } from '../../api/reports'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { useNotification } from '../../context/NotificationContext'
import './ReportButton.css'

type Props = {
  trackId: number
}

export function ReportButton({ trackId }: Props) {
  const { listener } = useCurrentListener()
  const { notify } = useNotification()
  const [loaded, setLoaded] = useState<{ trackId: number; status: ReportStatus } | null>(null)
  const status = loaded?.trackId === trackId ? loaded.status : null
  const listenerId = listener?.id

  useEffect(() => {
    if (listenerId === undefined) return
    const controller = new AbortController()
    fetchReportStatus(trackId, listenerId, controller.signal)
      .then((value) => setLoaded({ trackId, status: value }))
      .catch(() => {})
    return () => controller.abort()
  }, [trackId, listenerId])

  if (!listener) return null

  const mine = status?.reportedByListener ?? false
  const flagged = (status?.reports ?? 0) > 0

  function toggle() {
    if (!listener) return
    const request = mine ? cancelReport(trackId, listener.id) : reportTrack(trackId, listener.id)
    request
      .then(() => fetchReportStatus(trackId, listener.id))
      .then((value) => setLoaded({ trackId, status: value }))
      .catch(() => notify(mine ? "Le signalement n'a pas pu être annulé." : "Le signalement n'a pas pu être envoyé.", 'error'))
  }

  return (
    <button
      type="button"
      className={flagged ? 'report-button report-button-flagged' : 'report-button'}
      aria-pressed={mine}
      title="Le son joué ne correspond pas au nom affiché"
      onClick={toggle}
    >
      {mine ? 'Signalée (annuler)' : flagged ? 'Déjà signalée (signaler aussi)' : 'Signaler un mauvais son'}
    </button>
  )
}
