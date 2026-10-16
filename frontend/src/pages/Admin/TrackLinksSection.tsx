import { useEffect, useState, type FormEvent } from 'react'
import { removeTrackLink, replaceTrackLink, type LinkKind } from '../../api/admin'
import { fetchTracks, type Track } from '../../api/tracks'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useNotification } from '../../context/NotificationContext'
import { useAdminErrorHandler } from './useAdminErrorHandler'

const LABELS: Record<LinkKind, string> = {
  khinsider: 'KHInsider',
  youtube: 'YouTube',
}

type Props = {
  token: string
  onSessionExpired: () => void
  selected: Track | null
  onSelect: (track: Track | null) => void
  onLinkChanged: () => void
}

export function TrackLinksSection({ token, onSessionExpired, selected, onSelect, onLinkChanged }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [tracks, setTracks] = useState<Track[]>([])

  useEffect(() => {
    const controller = new AbortController()
    fetchTracks(controller.signal)
      .then(setTracks)
      .catch(() => {
        if (!controller.signal.aborted) notify('Impossible de charger les musiques.', 'error')
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function applyUpdate(updated: Track, message: string) {
    onSelect(updated)
    onLinkChanged()
    setTracks((current) => current.map((track) => (track.id === updated.id ? updated : track)))
    notify(message, 'success')
  }

  function save(track: Track, kind: LinkKind, link: string) {
    replaceTrackLink(token, track.id, kind, link)
      .then((updated) => {
        const playable = kind !== 'khinsider' || updated.audioLink !== null
        applyUpdate(
          updated,
          playable
            ? `Lien ${LABELS[kind]} de « ${track.name} » enregistré.`
            : `Lien ${LABELS[kind]} de « ${track.name} » enregistré, mais la musique n'est pas jouable pour l'instant.`,
        )
      })
      .catch(handleError)
  }

  function remove(track: Track, kind: LinkKind) {
    if (!window.confirm(`Retirer le lien ${LABELS[kind]} de « ${track.name} » ?`)) return
    removeTrackLink(token, track.id, kind)
      .then((updated) => applyUpdate(updated, `Lien ${LABELS[kind]} de « ${track.name} » retiré.`))
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Liens d'une musique</h2>
      <NamePicker
        items={tracks}
        selected={selected}
        onSelect={onSelect}
        onClear={() => onSelect(null)}
        placeholder="Chercher une musique"
        renderLabel={(track) => `${track.name} (${track.gameName})`}
        getSearchText={(track) => `${track.name} ${track.gameName} ${track.franchiseName}`}
      />
      {selected &&
        (['khinsider', 'youtube'] as const).map((kind) => {
          const current = kind === 'khinsider' ? selected.khinsiderLink : selected.youtubeLink
          return (
            <LinkRow
              key={`${selected.id}-${kind}-${current}`}
              label={LABELS[kind]}
              current={current}
              onSave={(link) => save(selected, kind, link)}
              onRemove={() => remove(selected, kind)}
            />
          )
        })}
    </section>
  )
}

type LinkRowProps = {
  label: string
  current: string | null
  onSave: (link: string) => void
  onRemove: () => void
}

function LinkRow({ label, current, onSave, onRemove }: LinkRowProps) {
  const [draft, setDraft] = useState(current ?? '')
  const trimmed = draft.trim()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSave(trimmed)
  }

  return (
    <form className="admin-row" onSubmit={handleSubmit}>
      <span className="admin-link-label">{label}</span>
      <input
        type="url"
        aria-label={`Lien ${label}`}
        placeholder="Aucun lien"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
      />
      <button type="submit" disabled={trimmed === '' || trimmed === current}>
        Enregistrer
      </button>
      <button type="button" onClick={onRemove} disabled={current === null}>
        Retirer
      </button>
    </form>
  )
}
