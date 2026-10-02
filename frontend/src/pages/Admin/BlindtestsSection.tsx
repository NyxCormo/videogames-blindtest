import { useEffect, useState } from 'react'
import { AdminSessionExpired, deleteItem } from '../../api/admin'
import { fetchBlindtests, type Blindtest } from '../../api/blindtests'
import { useNotification } from '../../context/NotificationContext'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function BlindtestsSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const [blindtests, setBlindtests] = useState<Blindtest[]>([])

  function load(signal?: AbortSignal) {
    return fetchBlindtests(signal).then(setBlindtests)
  }

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal).catch(() => {
      if (!controller.signal.aborted) notify('Impossible de charger les blindtests.', 'error')
    })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function remove(blindtest: Blindtest) {
    if (!window.confirm(`Supprimer le blindtest « ${blindtest.name} » ? Ses scores et son classement seront perdus.`)) return
    deleteItem(token, 'blindtests', blindtest.id)
      .then(() => {
        notify(`Blindtest « ${blindtest.name} » supprimé.`, 'success')
        return load()
      })
      .catch((err: Error) => {
        if (err instanceof AdminSessionExpired) {
          onSessionExpired()
        }
        notify(err.message, 'error')
      })
  }

  return (
    <section className="admin-section">
      <h2>Blindtests</h2>
      {blindtests.length === 0 ? (
        <p>Aucun blindtest.</p>
      ) : (
        <ul className="admin-list">
          {blindtests.map((blindtest) => (
            <li key={blindtest.id}>
              <span>
                {blindtest.name} <span className="admin-muted">({new Date(blindtest.createdAt).toLocaleDateString('fr-FR')})</span>
              </span>
              <button type="button" onClick={() => remove(blindtest)}>
                Supprimer
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
