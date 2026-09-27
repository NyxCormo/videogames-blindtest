import type { FormEvent } from 'react'
import type { Track } from '../../api/tracks'

type LinkKind = 'khinsider' | 'youtube'

const LABELS: Record<LinkKind, string> = {
  khinsider: 'KHInsider',
  youtube: 'YouTube',
}

type Props = {
  track: Track
  kind: LinkKind
  addingFor: number | null
  draft: string
  onStartAdding: () => void
  onDraftChange: (value: string) => void
  onCancel: () => void
  onSubmit: (event: FormEvent) => void
}

// Un badge si le lien existe déjà (jamais modifiable ici, réservé à un futur panneau admin), sinon un
// bouton qui ouvre un petit formulaire pour l'ajouter.
export function TrackLinkControl({ track, kind, addingFor, draft, onStartAdding, onDraftChange, onCancel, onSubmit }: Props) {
  const link = kind === 'khinsider' ? track.khinsiderLink : track.youtubeLink
  const label = LABELS[kind]

  if (link) {
    return (
      <a className="enrichment-badge" href={link} target="_blank" rel="noreferrer">
        {label}
      </a>
    )
  }

  if (addingFor === track.id) {
    return (
      <form className="enrichment-add-form" onSubmit={onSubmit}>
        <input
          type="url"
          placeholder={`Lien ${label}`}
          aria-label={`Lien ${label} pour ${track.name}`}
          value={draft}
          onChange={(event) => onDraftChange(event.target.value)}
          autoFocus
        />
        <button type="submit">Valider</button>
        <button type="button" onClick={onCancel}>
          Annuler
        </button>
      </form>
    )
  }

  return (
    <button type="button" onClick={onStartAdding}>
      + {label}
    </button>
  )
}
