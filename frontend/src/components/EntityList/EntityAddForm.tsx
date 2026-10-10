import { useState, type FormEvent, type KeyboardEvent } from 'react'

type Props = {
  label: string
  onAdd: (name: string) => Promise<unknown>
}

// Un bouton qui se transforme en champ de saisie : Entrée valide, Échap annule.
export function EntityAddForm({ label, onAdd }: Props) {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const trimmed = draft.trim()

  function close() {
    setOpen(false)
    setDraft('')
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (trimmed === '') return
    setSaving(true)
    onAdd(trimmed)
      .then(close)
      .catch(() => {})
      .finally(() => setSaving(false))
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') close()
  }

  if (!open) {
    return (
      <button type="button" className="entity-add-toggle" onClick={() => setOpen(true)}>
        + {label}
      </button>
    )
  }

  return (
    <form className="entity-add-form" onSubmit={handleSubmit}>
      <input
        type="text"
        aria-label={label}
        placeholder={label}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={saving}
        autoFocus
      />
      <button type="submit" disabled={saving || trimmed === ''}>
        {saving ? '…' : 'Créer'}
      </button>
      <button type="button" onClick={close} disabled={saving}>
        Annuler
      </button>
    </form>
  )
}
