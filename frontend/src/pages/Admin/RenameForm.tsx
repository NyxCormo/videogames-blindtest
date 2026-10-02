import { useState, type FormEvent } from 'react'

type RenameFormProps = {
  current: string
  onRename: (name: string) => void
}

export function RenameForm({ current, onRename }: RenameFormProps) {
  const [draft, setDraft] = useState(current)
  const trimmed = draft.trim()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onRename(trimmed)
  }

  return (
    <form className="admin-row" onSubmit={handleSubmit}>
      <input
        type="text"
        aria-label="Nouveau nom"
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
      />
      <button type="submit" disabled={trimmed === '' || trimmed === current}>
        Renommer
      </button>
    </form>
  )
}
