import { useState, type ReactNode } from 'react'
import { NamePicker } from '../../components/NamePicker/NamePicker'

export type Named = { id: number; name: string }

type MergeFormProps<T extends Named> = {
  items: T[]
  renderLabel?: (item: T) => ReactNode
  getSearchText?: (item: T) => string
  onMerge: (target: T) => void
}

export function MergeForm<T extends Named>({ items, renderLabel, getSearchText, onMerge }: MergeFormProps<T>) {
  const [target, setTarget] = useState<T | null>(null)

  return (
    <div className="admin-row">
      <NamePicker
        items={items}
        selected={target}
        onSelect={setTarget}
        onClear={() => setTarget(null)}
        placeholder="Fusionner dans…"
        renderLabel={renderLabel}
        getSearchText={getSearchText}
      />
      <button type="button" disabled={target === null} onClick={() => target && onMerge(target)}>
        Fusionner
      </button>
    </div>
  )
}
