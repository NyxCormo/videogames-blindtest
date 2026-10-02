import { useEffect, useState } from 'react'
import { AdminSessionExpired, fetchRefreshStatus, startRefresh, type RefreshStatus } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'

const POLL_DELAY_MS = 3000

type Props = {
  token: string
  onSessionExpired: () => void
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

export function AudioLinksSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const [status, setStatus] = useState<RefreshStatus | null>(null)

  function handleError(err: Error) {
    if (err instanceof AdminSessionExpired) {
      onSessionExpired()
    }
    notify(err.message, 'error')
  }

  useEffect(() => {
    fetchRefreshStatus(token).then(setStatus).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Tant qu'un balayage tourne, on redemande où il en est toutes les quelques secondes.
  useEffect(() => {
    if (!status?.running) return
    const timeout = setTimeout(() => {
      fetchRefreshStatus(token)
        .then((next) => {
          setStatus(next)
          if (!next.running) notify('Rafraîchissement des liens terminé.', 'success')
        })
        .catch(handleError)
    }, POLL_DELAY_MS)
    return () => clearTimeout(timeout)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  function handleStart() {
    startRefresh(token)
      .then((next) => {
        setStatus(next)
        notify('Rafraîchissement lancé : environ une seconde par musique.', 'success')
      })
      .catch(handleError)
  }

  const report = status?.lastReport

  return (
    <section className="admin-section">
      <h2>Liens audio</h2>
      <div className="admin-row">
        <button type="button" onClick={handleStart} disabled={status === null || status.running}>
          Rafraîchir tous les liens
        </button>
      </div>
      {status?.running && status.startedAt && <p>Rafraîchissement en cours depuis {formatDate(status.startedAt)}…</p>}
      {!status?.running && report && status?.finishedAt && (
        <p>
          Dernier rafraîchissement terminé le {formatDate(status.finishedAt)} : {report.checked} musiques vérifiées,{' '}
          {report.alive} liens encore bons, {report.refreshed} rafraîchis, {report.failed} en échec.
        </p>
      )}
      {status !== null && !status.running && !report && (
        <p className="admin-muted">Aucun rafraîchissement depuis le démarrage du serveur.</p>
      )}
    </section>
  )
}
