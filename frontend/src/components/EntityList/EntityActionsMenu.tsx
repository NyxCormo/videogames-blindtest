import { useState, type KeyboardEvent } from 'react'

type Props = {
  onRename: () => void
  onDelete: () => void
  disabled?: boolean
}

// Les actions restent cachées derrière « ⋯ » pour éviter un clic par erreur dans une longue liste.
export function EntityActionsMenu({ onRename, onDelete, disabled = false }: Props) {
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
      <div className="entity-actions" onKeyDown={closeOnEscape}>
        <button
          type="button"
          className="entity-actions-toggle"
          aria-label={open ? 'Fermer les actions' : 'Actions'}
          aria-expanded={open}
          onClick={() => setOpen(!open)}
          disabled={disabled}
        >
          {open ? '×' : '⋯'}
        </button>
      </div>
      {open && (
        <div className="entity-actions-menu" onKeyDown={closeOnEscape}>
          <button type="button" onClick={() => choose(onRename)} autoFocus>
            Renommer
          </button>
          <button type="button" className="entity-danger" onClick={() => choose(onDelete)}>
            Supprimer
          </button>
        </div>
      )}
    </>
  )
}
