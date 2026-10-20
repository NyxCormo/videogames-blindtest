import { useEffect, useState } from 'react'
import { fetchCoverFillStatus, startCoverFill, type CoverFillStatus } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'
import { plural } from '../../plural'
import { useAdminErrorHandler } from './useAdminErrorHandler'

const POLL_DELAY_MS = 3000

type Props = {
  token: string
  onSessionExpired: () => void
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

export function CoverFillSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [status, setStatus] = useState<CoverFillStatus | null>(null)

  useEffect(() => {
    fetchCoverFillStatus(token).then(setStatus).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!status?.running) return
    const timeout = setTimeout(() => {
      fetchCoverFillStatus(token)
        .then((next) => {
          setStatus(next)
          if (!next.running) notify('Recherche des images terminée.', 'success')
        })
        .catch(handleError)
    }, POLL_DELAY_MS)
    return () => clearTimeout(timeout)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  function handleStart() {
    startCoverFill(token)
      .then((next) => {
        setStatus(next)
        notify('Recherche lancée : environ une seconde par jeu.', 'success')
      })
      .catch(handleError)
  }

  const report = status?.lastReport

  return (
    <section className="admin-section">
      <h2>Récupération automatique</h2>
      <p className="muted">
        Un jeu sans image reçoit la pochette de l'album de sa première musique jouable, cherchée sous les noms habituels
        (00 Front.jpg, Cover.jpg, folder.jpg…). Les images sont ensuite téléchargées sur le serveur.
      </p>
      <div className="admin-row">
        <button type="button" onClick={handleStart} disabled={status === null || status.running}>
          Récupérer les images manquantes
        </button>
      </div>
      {status?.running && status.startedAt && <p>Recherche en cours depuis {formatDate(status.startedAt)}…</p>}
      {!status?.running && report && status?.finishedAt && (
        <p>
          Dernière recherche terminée le {formatDate(status.finishedAt)} : {plural(report.checked, 'jeu traité', 'jeux traités')},{' '}
          {plural(report.found, 'image trouvée', 'images trouvées')}, {plural(report.downloaded, 'téléchargée', 'téléchargées')},{' '}
          {report.failed} en échec.
        </p>
      )}
    </section>
  )
}
