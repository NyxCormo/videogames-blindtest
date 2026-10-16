import { useState, type ReactNode } from 'react'
import { EntityAddForm } from './EntityAddForm'
import { EntityRow, type RowMerge } from './EntityRow'
import { filterByText, sortByText } from './listText'
import './EntityList.css'

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
  // État de la case « Sélectionner pour fusion » de chaque ligne ; absent, la liste n'a pas de case.
  mergeFor?: (item: T) => RowMerge
  extra?: (item: T) => ReactNode
  // Bouton « + … » en bas de la liste ; la promesse rejetée laisse le champ ouvert pour corriger.
  addLabel?: string
  onAdd?: (name: string) => Promise<unknown>
  // false : pas de champ « Filtrer », quand la page filtre déjà la liste elle-même.
  filterable?: boolean
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
  mergeFor,
  extra,
  addLabel,
  onAdd,
  filterable = true,
  emptyText,
  sorted = true,
}: Props<T>) {
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
      {filterable && (
        <input
          type="search"
          placeholder="Filtrer"
          aria-label={`Filtrer : ${title}`}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          disabled={items === null || items.length === 0}
        />
      )}
      {items === null ? (
        <p className="muted">Chargement…</p>
      ) : shown.length === 0 ? (
        <p className="muted">{items.length === 0 ? emptyText : 'Aucun résultat pour ce filtre.'}</p>
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
              merge={mergeFor?.(item)}
              extra={extra?.(item)}
            />
          ))}
        </ul>
      )}
      {addLabel && onAdd && items !== null && <EntityAddForm label={addLabel} onAdd={onAdd} />}
    </div>
  )
}
