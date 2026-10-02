import { useState, type KeyboardEvent } from 'react'

type Props = {
  onRename: () => void
  onDelete: () => void
  disabled?: boolean
}

// Les actions restent cachées derrière « ⋯ » pour éviter un clic par erreur dans une longue liste.
export function AdminActionsMenu({ onRename, onDelete, disabled = false }: Props) {
  const [open, setOpen] = useState(false)

  function choose(action: () => void) {
    setOpen(false)
    action()
  }

  function closeOnEscape(event: KeyboardEvent) {
    if (event.key === 'Escape') setOpen(false)
  }

  return (
    <>
      <div className="admin-actions" onKeyDown={closeOnEscape}>
        <button
          type="button"
          className="admin-actions-toggle"
          aria-label={open ? 'Fermer les actions' : 'Actions admin'}
          aria-expanded={open}
          onClick={() => setOpen(!open)}
          disabled={disabled}
        >
          {open ? '×' : '⋯'}
        </button>
      </div>
      {open && (
        <div className="admin-actions-menu" onKeyDown={closeOnEscape}>
          <button type="button" onClick={() => choose(onRename)} autoFocus>
            Renommer
          </button>
          <button type="button" className="admin-danger" onClick={() => choose(onDelete)}>
            Supprimer
          </button>
        </div>
      )}
    </>
  )
}
