import { type ReactNode, useState } from 'react'
import './NamePicker.css'

// T : franchise, jeu ou musique ont tous au moins un id et un nom.
type NamedItem = {
  id: number
  name: string
}

type Props<T extends NamedItem> = {
  items: T[]
  selected: T | null
  onSelect: (item: T) => void
  onClear: () => void
  placeholder: string
  disabled?: boolean
  // Par défaut, on affiche et on filtre sur item.name. Utile pour un jeu : afficher "Nom (Franchise)"
  // et pouvoir aussi le retrouver en cherchant la franchise.
  renderLabel?: (item: T) => ReactNode
  getSearchText?: (item: T) => string
}

// Choix strict dans une liste filtrée en tapant : impossible de valider autre chose qu'un élément existant.
export function NamePicker<T extends NamedItem>({
  items,
  selected,
  onSelect,
  onClear,
  placeholder,
  disabled,
  renderLabel,
  getSearchText,
}: Props<T>) {
  const [query, setQuery] = useState('')

  if (selected) {
    return (
      <div className="name-picker">
        <span className="name-picker-selected">{selected.name}</span>
        <button type="button" onClick={onClear} disabled={disabled}>
          Changer
        </button>
      </div>
    )
  }

  const trimmed = query.trim().toLowerCase()
  const results =
    trimmed.length === 0
      ? []
      : items.filter((item) => (getSearchText ? getSearchText(item) : item.name).toLowerCase().includes(trimmed))

  return (
    <div className="name-picker">
      <input
        type="search"
        placeholder={placeholder}
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        disabled={disabled}
      />
      {results.length > 0 && (
        <ul className="name-picker-results">
          {results.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                onClick={() => {
                  onSelect(item)
                  setQuery('')
                }}
              >
                {renderLabel ? renderLabel(item) : item.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
