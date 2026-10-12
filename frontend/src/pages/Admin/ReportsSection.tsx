import { useEffect, useState } from 'react'
import { dismissReports, fetchReportedTracks, type ReportedTrack } from '../../api/admin'
import type { Track } from '../../api/tracks'
import { useNotification } from '../../context/NotificationContext'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
  onFixLink: (track: Track) => void
}

export function ReportsSection({ token, onSessionExpired, onFixLink }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [reported, setReported] = useState<ReportedTrack[] | null>(null)

  useEffect(() => {
    fetchReportedTracks(token).then(setReported).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function dismiss(track: Track) {
    dismissReports(token, track.id)
      .then(() => {
        setReported((current) => current && current.filter((entry) => entry.track.id !== track.id))
        notify(`Signalements de « ${track.name} » retirés.`, 'success')
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Musiques signalées</h2>
      {reported === null && <p className="admin-muted">Chargement…</p>}
      {reported !== null && reported.length === 0 && <p className="admin-muted">Aucune musique signalée.</p>}
      {reported !== null && reported.length > 0 && (
        <>
          <p className="admin-muted">Changer ou retirer le lien KHInsider d'une musique retire ses signalements.</p>
          <ul className="admin-reports">
            {reported.map(({ track, reporters }) => (
              <li key={track.id}>
                <span>
                  <strong>{track.name}</strong> ({track.gameName}, {track.franchiseName})
                </span>
                <span className="admin-muted">Signalée par {reporters.join(', ')}</span>
                <div className="admin-row">
                  {track.audioLink && <audio controls preload="none" src={track.audioLink} />}
                  <button type="button" onClick={() => onFixLink(track)}>
                    Corriger le lien
                  </button>
                  <button type="button" onClick={() => dismiss(track)}>
                    Retirer les signalements
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
