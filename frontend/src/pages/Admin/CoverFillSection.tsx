import { useEffect, useState } from 'react'
import { fetchCoverFillStatus, startCoverFill, type CoverFillMode, type CoverFillStatus } from '../../api/admin'
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

  function handleStart(mode: CoverFillMode) {
    startCoverFill(token, mode)
      .then((next) => {
        setStatus(next)
        notify('Recherche lancée : environ huit secondes par album à chercher.', 'success')
      })
      .catch(handleError)
  }

  const report = status?.lastReport

  return (
    <section className="admin-section">
      <h2>Récupération automatique</h2>
      <p className="muted">
        Les pochettes sont cherchées sur le CDN des MP3, dans le dossier de chaque album, sous les noms habituels (00 Front.jpg,
        Cover.jpg, folder.jpg…). Le résultat de chaque album est gardé : un jeu sans image reçoit la première pochette trouvée
        parmi ses albums, puis les images sont téléchargées sur le serveur.
      </p>
      {status && (
        <p>
          {plural(status.albums, 'album déjà recherché', 'albums déjà recherchés')}, dont{' '}
          {status.albumsWithoutImage} sans pochette trouvée.
        </p>
      )}
      <div className="admin-row">
        <button type="button" onClick={() => handleStart('NEW')} disabled={status === null || status.running}>
          Chercher les nouveaux albums
        </button>
        <button type="button" onClick={() => handleStart('WITHOUT_IMAGE')} disabled={status === null || status.running}>
          Rechercher aussi les albums sans pochette
        </button>
        <button type="button" onClick={() => handleStart('ALL')} disabled={status === null || status.running}>
          Tout rechercher à nouveau
        </button>
      </div>
      {status?.running && status.startedAt && (
        <p>
          Recherche en cours depuis {formatDate(status.startedAt)}
          {report &&
            ` : ${report.scanned} / ${plural(report.toScan, 'album recherché', 'albums recherchés')}, ${plural(report.covered, 'jeu a reçu une image', 'jeux ont reçu une image')}`}
          …
        </p>
      )}
      {!status?.running && report && status?.finishedAt && (
        <p>
          Dernière recherche terminée le {formatDate(status.finishedAt)} :{' '}
          {plural(report.scanned, 'album recherché', 'albums recherchés')},{' '}
          {plural(report.covered, 'jeu a reçu une image', 'jeux ont reçu une image')} ;{' '}
          {plural(report.downloaded, 'image', 'images')} sur le serveur,{' '}
          {plural(report.notDownloaded, 'non téléchargée', 'non téléchargées')}.
        </p>
      )}
    </section>
  )
}
