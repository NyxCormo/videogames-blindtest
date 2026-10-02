import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { AdminActionsMenu } from './AdminActionsMenu'
import type { MergeSlot } from './mergeSelection'

type Props = {
  label: string
  detail?: string
  active?: boolean
  onOpen?: () => void
  onRename?: (name: string) => Promise<unknown>
  onDelete?: () => void
  // Absent : pas de fusion pour ce type (les blindtests).
  merge?: { slot: MergeSlot | null; disabled: boolean; onToggle: () => void }
}

export function EntityRow({ label, detail, active = false, onOpen, onRename, onDelete, merge }: Props) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(label)
  const [saving, setSaving] = useState(false)
  const trimmed = draft.trim()

  function startEditing() {
    setDraft(label)
    setEditing(true)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!onRename || trimmed === '') return
    if (trimmed === label) {
      setEditing(false)
      return
    }
    setSaving(true)
    onRename(trimmed)
      .then(() => setEditing(false))
      .catch(() => {})
      .finally(() => setSaving(false))
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      event.stopPropagation()
      setEditing(false)
    }
  }

  const content = (
    <>
      <span className="entity-row-label">{label}</span>
      {detail && <span className="entity-row-detail">{detail}</span>}
    </>
  )

  return (
    <li
      className={['entity-row', active && 'entity-row-active', merge?.slot && `entity-row-merge-${merge.slot}`]
        .filter(Boolean)
        .join(' ')}
    >
      {merge && (
        <input
          type="checkbox"
          className="entity-row-merge"
          aria-label={`Sélectionner ${label} pour fusion`}
          title="Sélectionner pour fusion"
          checked={merge.slot !== null}
          disabled={merge.disabled}
          onChange={merge.onToggle}
        />
      )}
      {editing ? (
        <form className="entity-row-edit" onSubmit={handleSubmit}>
          <input
            type="text"
            aria-label={`Nouveau nom pour ${label}`}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={handleKeyDown}
            disabled={saving}
            autoFocus
          />
          <button type="submit" disabled={saving || trimmed === ''}>
            {saving ? '…' : 'OK'}
          </button>
          {trimmed === '' && <span className="entity-row-error">Le nom ne peut pas être vide.</span>}
        </form>
      ) : onOpen ? (
        <button type="button" className="entity-row-main" aria-pressed={active} onClick={onOpen}>
          {content}
        </button>
      ) : (
        <div className="entity-row-main">{content}</div>
      )}
      {!editing && onRename && onDelete && <AdminActionsMenu onRename={startEditing} onDelete={onDelete} />}
    </li>
  )
}
