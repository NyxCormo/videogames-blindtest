import { Dialog } from '@base-ui/react/dialog'
import { useState, type FormEvent } from 'react'
import { proposeCover } from '../../api/games'
import type { Listener } from '../../api/listeners'
import './CoverProposalDialog.css'

type Props = {
  open: boolean
  onOpenChange: (open: boolean) => void
  gameId: number
  gameName: string
  hasCover: boolean
  listener: Listener
  onSent: () => void
}

export function CoverProposalDialog({ open, onOpenChange, gameId, gameName, hasCover, listener, onSent }: Props) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Backdrop className="dialog-backdrop" />
        <Dialog.Popup className="dialog">
          <div className="dialog-header">
            <Dialog.Title className="dialog-title">Image de {gameName}</Dialog.Title>
            <Dialog.Close className="dialog-close" aria-label="Fermer">
              ×
            </Dialog.Close>
          </div>
          <ProposalForm gameId={gameId} hasCover={hasCover} listener={listener} onSent={onSent} />
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

type FormProps = {
  gameId: number
  hasCover: boolean
  listener: Listener
  onSent: () => void
}

function ProposalForm({ gameId, hasCover, listener, onSent }: FormProps) {
  const [url, setUrl] = useState('')
  const [loaded, setLoaded] = useState<{ url: string; ok: boolean } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const trimmed = url.trim()
  const preview = loaded?.url === trimmed ? (loaded.ok ? 'ok' : 'broken') : 'loading'
  const canSend = trimmed === '' ? hasCover : preview === 'ok'

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!canSend) return
    setSending(true)
    setError(null)
    proposeCover(gameId, listener.id, trimmed === '' ? null : trimmed)
      .then(onSent)
      .catch((err: Error) => setError(err.message))
      .finally(() => setSending(false))
  }

  return (
    <form className="cover-proposal" onSubmit={handleSubmit}>
      <label>
        Adresse de l'image
        <input
          type="url"
          placeholder="https://…"
          value={url}
          onChange={(event) => setUrl(event.target.value)}
          autoFocus
        />
      </label>
      <p className="muted">
        Sur une page qui montre la bonne image : clic droit sur l'image, puis « Copier l'adresse de l'image ».
        {hasCover && " Sans adresse, la proposition signale seulement que l'image actuelle ne va pas."}
      </p>
      {trimmed !== '' && (
        <div className="cover-proposal-preview">
          <img
            key={trimmed}
            src={trimmed}
            alt="Aperçu de l'image proposée"
            hidden={preview !== 'ok'}
            onLoad={() => setLoaded({ url: trimmed, ok: true })}
            onError={() => setLoaded({ url: trimmed, ok: false })}
          />
          {preview === 'loading' && <p className="muted">Chargement de l'aperçu…</p>}
          {preview === 'broken' && <p className="cover-proposal-error">Cette adresse ne donne pas une image.</p>}
        </div>
      )}
      {error && (
        <p role="alert" className="cover-proposal-error">
          {error}
        </p>
      )}
      <div className="cover-proposal-actions">
        <button type="submit" className="button-primary" disabled={!canSend || sending}>
          {trimmed === '' && hasCover ? "Signaler que l'image ne va pas" : 'Proposer cette image'}
        </button>
      </div>
    </form>
  )
}
