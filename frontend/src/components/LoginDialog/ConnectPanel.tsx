import { useEffect, useState, type KeyboardEvent } from 'react'
import { searchListeners, type Listener } from '../../api/listeners'
import { useCurrentListener } from '../../context/CurrentListenerContext'

type Props = {
  onChoose: (listener: Listener) => void
  onCreate: (name: string) => void
}

export function ConnectPanel({ onChoose, onCreate }: Props) {
  const { recent, forgetRecent } = useCurrentListener()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<{ query: string; listeners: Listener[] } | null>(null)
  const [active, setActive] = useState(0)
  const [failed, setFailed] = useState<string | null>(null)
  const trimmed = query.trim()
  const found = results && results.query === trimmed ? results.listeners : null

  useEffect(() => {
    if (trimmed.length < 2) return
    const controller = new AbortController()
    const timeout = setTimeout(() => {
      searchListeners(trimmed, controller.signal)
        .then((listeners) => {
          setResults({ query: trimmed, listeners })
          setActive(0)
          setFailed(null)
        })
        .catch(() => {
          if (!controller.signal.aborted) setFailed(trimmed)
        })
    }, 250)
    return () => {
      clearTimeout(timeout)
      controller.abort()
    }
  }, [trimmed])

  function handleKeyDown(event: KeyboardEvent) {
    if (!found || found.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((index) => Math.min(index + 1, found.length - 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((index) => Math.max(index - 1, 0))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      onChoose(found[active])
    }
  }

  return (
    <div className="login-panel">
      {recent.length > 0 && (
        <div className="login-recent">
          <p className="muted">Déjà utilisés sur cet appareil</p>
          <div className="login-recent-list">
            {recent.map((listener) => (
              <span key={listener.id} className="login-recent-item">
                <button type="button" onClick={() => onChoose(listener)}>
                  {listener.name}
                </button>
                <button
                  type="button"
                  className="login-recent-forget"
                  aria-label={`Retirer ${listener.name} de cet appareil`}
                  title="Retirer de cet appareil"
                  onClick={() => forgetRecent(listener.id)}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        </div>
      )}
      <input
        type="search"
        placeholder="Chercher son pseudo"
        aria-label="Chercher son pseudo"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        onKeyDown={handleKeyDown}
        autoFocus
      />
      {failed === trimmed && (
        <p role="alert" className="login-error">
          La recherche de pseudo a échoué.
        </p>
      )}
      {trimmed.length >= 2 && found && found.length > 0 && (
        <ul className="login-results" role="listbox" aria-label="Pseudos trouvés">
          {found.map((listener, index) => (
            <li key={listener.id} role="option" aria-selected={index === active}>
              <button type="button" tabIndex={-1} onMouseEnter={() => setActive(index)} onClick={() => onChoose(listener)}>
                {listener.name}
              </button>
            </li>
          ))}
        </ul>
      )}
      {trimmed.length >= 2 && found && found.length === 0 && (
        <p className="muted">
          Aucun pseudo ne correspond.{' '}
          <button type="button" className="login-link" onClick={() => onCreate(trimmed)}>
            Créer « {trimmed} »
          </button>
        </p>
      )}
    </div>
  )
}
