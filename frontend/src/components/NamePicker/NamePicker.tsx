import { type KeyboardEvent, type ReactNode, useRef, useState } from 'react'
import { matchesWords, searchWords } from '../../textSearch'
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
  autoFocus?: boolean
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
  autoFocus,
  renderLabel,
  getSearchText,
}: Props<T>) {
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const listRef = useRef<HTMLUListElement>(null)

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

  const words = searchWords(query)
  const results =
    words.length === 0
      ? []
      : items.filter((item) => matchesWords(getSearchText ? getSearchText(item) : item.name, words))
  const current = Math.min(active, results.length - 1)

  function choose(item: T) {
    onSelect(item)
    setQuery('')
    setActive(0)
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (results.length === 0) return
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const next = event.key === 'ArrowDown' ? Math.min(current + 1, results.length - 1) : Math.max(current - 1, 0)
      setActive(next)
      listRef.current?.children[next]?.scrollIntoView({ block: 'nearest' })
    } else if (event.key === 'Enter') {
      event.preventDefault()
      choose(results[current])
    }
  }

  return (
    <div className="name-picker">
      <input
        type="search"
        placeholder={placeholder}
        value={query}
        onChange={(event) => {
          setQuery(event.target.value)
          setActive(0)
        }}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        autoFocus={autoFocus}
      />
      {results.length > 0 && (
        <ul ref={listRef} className="results-list name-picker-results" role="listbox">
          {results.map((item, index) => (
            <li key={item.id} role="option" aria-selected={index === current}>
              <button type="button" onMouseEnter={() => setActive(index)} onClick={() => choose(item)}>
                {renderLabel ? renderLabel(item) : item.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
