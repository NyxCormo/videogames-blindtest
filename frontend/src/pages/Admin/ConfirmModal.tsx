import type { ReactNode } from 'react'

type Props = {
  title: string
  children: ReactNode
  confirmLabel: string
  // null : pas de bouton de confirmation, seulement « Fermer » (action refusée)
  onConfirm: (() => void) | null
  onCancel: () => void
  busy?: boolean
}

export function ConfirmModal({ title, children, confirmLabel, onConfirm, onCancel, busy = false }: Props) {
  return (
    <div className="admin-modal-backdrop" onKeyDown={(event) => event.key === 'Escape' && !busy && onCancel()}>
      <div className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="admin-modal-title">
        <h2 id="admin-modal-title">{title}</h2>
        <div className="admin-modal-body">{children}</div>
        <div className="admin-modal-actions">
          <button type="button" onClick={onCancel} disabled={busy} autoFocus={onConfirm === null}>
            {onConfirm === null ? 'Fermer' : 'Annuler'}
          </button>
          {onConfirm !== null && (
            <button type="button" className="admin-danger" onClick={onConfirm} disabled={busy} autoFocus>
              {busy ? 'En cours…' : confirmLabel}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
