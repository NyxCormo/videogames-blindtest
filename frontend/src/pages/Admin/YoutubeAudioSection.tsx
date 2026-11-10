import { useEffect, useState } from 'react'
import { fetchYoutubeAudioStatus, startYoutubeAudioDownload, type YoutubeAudioStatus } from '../../api/admin'
import { fetchTrack, type Track } from '../../api/tracks'
import { useNotification } from '../../context/NotificationContext'
import { plural } from '../../plural'
import { useAdminErrorHandler } from './useAdminErrorHandler'

const POLL_DELAY_MS = 3000

type Props = {
  token: string
  onSessionExpired: () => void
  onFixLink: (track: Track) => void
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

export function YoutubeAudioSection({ token, onSessionExpired, onFixLink }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [status, setStatus] = useState<YoutubeAudioStatus | null>(null)

  useEffect(() => {
    fetchYoutubeAudioStatus(token).then(setStatus).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!status?.running) return
    const timeout = setTimeout(() => {
      fetchYoutubeAudioStatus(token)
        .then((next) => {
          setStatus(next)
          if (!next.running) notify('Téléchargement des audios YouTube terminé.', 'success')
        })
        .catch(handleError)
    }, POLL_DELAY_MS)
    return () => clearTimeout(timeout)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status])

  function handleStart() {
    startYoutubeAudioDownload(token)
      .then((next) => {
        setStatus(next)
        notify('Téléchargement lancé.', 'success')
      })
      .catch(handleError)
  }

  function fixLink(trackId: number) {
    fetchTrack(trackId).then(onFixLink).catch(handleError)
  }

  const report = status?.lastReport

  return (
    <section className="admin-section">
      <h2>Audios YouTube</h2>
      <p className="muted">
        Une musique sans lien KHInsider, ou dont le lien KHInsider ne marche plus, est jouée depuis sa vidéo YouTube : l'audio
        est téléchargé sur le serveur à l'ajout du lien, et la vérification quotidienne télécharge ceux qui manquent.
      </p>
      {status && !status.available && (
        <p>
          Aucun téléchargeur YouTube sur ce serveur : les audios déjà téléchargés restent jouables, mais aucun nouveau ne sera
          téléchargé.
        </p>
      )}
      {status && (
        <p>
          {plural(status.tracks, 'musique à jouer', 'musiques à jouer')} depuis YouTube :{' '}
          {plural(status.downloaded, 'audio téléchargé', 'audios téléchargés')},{' '}
          {plural(status.failures.length, 'en échec', 'en échec')}.
        </p>
      )}
      <div className="admin-row">
        <button type="button" onClick={handleStart} disabled={status === null || status.running || !status.available}>
          Télécharger les audios manquants
        </button>
      </div>
      {status?.running && status.startedAt && (
        <p>
          Téléchargement en cours depuis {formatDate(status.startedAt)}
          {report && ` : ${report.done} / ${plural(report.toDownload, 'audio', 'audios')}`}…
        </p>
      )}
      {!status?.running && report && status?.finishedAt && (
        <p>
          Dernier téléchargement terminé le {formatDate(status.finishedAt)} :{' '}
          {plural(report.downloaded, 'audio téléchargé', 'audios téléchargés')},{' '}
          {plural(report.failed, 'en échec', 'en échec')}.
        </p>
      )}
      {status && status.failures.length > 0 && (
        <ul className="admin-reports">
          {status.failures.map((failure) => (
            <li key={failure.trackId}>
              <span>
                <strong>{failure.trackName}</strong> ({failure.gameName})
              </span>
              <span className="muted">{failure.error}</span>
              <div className="admin-row">
                <a href={failure.youtubeLink} target="_blank" rel="noreferrer">
                  Ouvrir la vidéo
                </a>
                <button type="button" onClick={() => fixLink(failure.trackId)}>
                  Corriger le lien
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
