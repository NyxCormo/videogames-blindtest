import { useState } from 'react'
import type { AdminKind } from '../../api/admin'
import { filterByText, sortByText } from './adminText'
import { EntityRow } from './EntityRow'
import { useMergeSelection } from './mergeSelection'

type Props<T extends { id: number }> = {
  title: string
  // null tant que la liste se charge
  items: T[] | null
  label: (item: T) => string
  detail?: (item: T) => string
  searchText?: (item: T) => string
  activeId?: number | null
  onOpen?: (item: T) => void
  onRename?: (item: T, name: string) => Promise<unknown>
  onDelete?: (item: T) => void
  // Type utilisé pour la fusion ; absent, la liste n'a pas de case « Sélectionner pour fusion ».
  mergeKind?: AdminKind
  // Nom affiché dans le bandeau de fusion, quand le nom seul ne suffit pas (deux musiques homonymes).
  mergeName?: (item: T) => string
  emptyText: string
  // Par défaut, tri alphabétique ; false garde l'ordre reçu (les blindtests, du plus récent au plus ancien).
  sorted?: boolean
}

export function EntityList<T extends { id: number }>({
  title,
  items,
  label,
  detail,
  searchText,
  activeId,
  onOpen,
  onRename,
  onDelete,
  mergeKind,
  mergeName,
  emptyText,
  sorted = true,
}: Props<T>) {
  const mergeSelection = useMergeSelection()
  const [query, setQuery] = useState('')
  const ordered = items === null ? [] : sorted ? sortByText(items, label) : items
  const shown = filterByText(ordered, query, searchText ?? label)

  return (
    <div className="entity-list">
      <div className="entity-list-header">
        <h3>{title}</h3>
        {items !== null && (
          <span className="entity-row-detail">
            {shown.length === items.length ? items.length : `${shown.length} / ${items.length}`}
          </span>
        )}
      </div>
      <input
        type="search"
        placeholder="Filtrer"
        aria-label={`Filtrer : ${title}`}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        disabled={items === null || items.length === 0}
      />
      {items === null ? (
        <p className="admin-muted">Chargement…</p>
      ) : shown.length === 0 ? (
        <p className="admin-muted">{items.length === 0 ? emptyText : 'Aucun résultat pour ce filtre.'}</p>
      ) : (
        <ul>
          {shown.map((item) => (
            <EntityRow
              key={item.id}
              label={label(item)}
              detail={detail?.(item)}
              active={item.id === activeId}
              onOpen={onOpen && (() => onOpen(item))}
              onRename={onRename && ((name) => onRename(item, name))}
              onDelete={onDelete && (() => onDelete(item))}
              merge={
                mergeKind && {
                  slot: mergeSelection.slotOf(mergeKind, item.id),
                  disabled: !mergeSelection.canSelect(mergeKind, item.id),
                  onToggle: () => mergeSelection.toggle(mergeKind, { id: item.id, name: (mergeName ?? label)(item) }),
                }
              }
            />
          ))}
        </ul>
      )}
    </div>
  )
}
