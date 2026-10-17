import { useState, type FormEvent } from 'react'
import { createListener, fetchSimilarListeners, type Listener } from '../../api/listeners'
import { useNotification } from '../../context/NotificationContext'
import { normalize } from '../../textSearch'

const MIN_LENGTH = 2
const MAX_LENGTH = 20

type Props = {
  initialName: string
  onDone: (listener: Listener) => void
}

type Step = { kind: 'typing' } | { kind: 'similar'; similar: Listener[] } | { kind: 'confirm' }

export function CreatePanel({ initialName, onDone }: Props) {
  const { notify } = useNotification()
  const [name, setName] = useState(initialName)
  const [step, setStep] = useState<Step>({ kind: 'typing' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const trimmed = name.trim().replace(/\s+/g, ' ')
  const length = [...trimmed].length
  const lengthOk = length >= MIN_LENGTH && length <= MAX_LENGTH

  function handleContinue(event: FormEvent) {
    event.preventDefault()
    if (!lengthOk) return
    setBusy(true)
    setError(null)
    fetchSimilarListeners(trimmed)
      .then((similar) => setStep(similar.length > 0 ? { kind: 'similar', similar } : { kind: 'confirm' }))
      .catch((err: Error) => setError(err.message))
      .finally(() => setBusy(false))
  }

  function handleCreate() {
    setBusy(true)
    setError(null)
    createListener(trimmed)
      .then((created) => {
        notify(`Pseudo « ${created.name} » créé.`, 'success')
        onDone(created)
      })
      .catch((err: Error) => {
        setError(err.message)
        setStep({ kind: 'typing' })
      })
      .finally(() => setBusy(false))
  }

  if (step.kind === 'similar') {
    const taken = step.similar.some((listener) => normalize(listener.name) === normalize(trimmed))
    return (
      <div className="login-panel">
        <p>{taken ? 'Ce pseudo existe déjà :' : 'Un pseudo proche existe déjà :'}</p>
        <div className="login-recent-list">
          {step.similar.map((listener) => (
            <button key={listener.id} type="button" onClick={() => onDone(listener)}>
              C'est moi, {listener.name}
            </button>
          ))}
        </div>
        <div className="login-actions">
          <button type="button" onClick={() => setStep({ kind: 'typing' })} disabled={busy}>
            Modifier
          </button>
          {!taken && (
            <button type="button" className="button-primary" onClick={handleCreate} disabled={busy}>
              Non, créer « {trimmed} »
            </button>
          )}
        </div>
      </div>
    )
  }

  if (step.kind === 'confirm') {
    return (
      <div className="login-panel">
        <p>
          Créer le pseudo <strong>{trimmed}</strong> ?
        </p>
        <div className="login-actions">
          <button type="button" onClick={() => setStep({ kind: 'typing' })} disabled={busy}>
            Modifier
          </button>
          <button type="button" className="button-primary" onClick={handleCreate} disabled={busy} autoFocus>
            Créer
          </button>
        </div>
      </div>
    )
  }

  return (
    <form className="login-panel" onSubmit={handleContinue}>
      <input
        type="text"
        placeholder="Nouveau pseudo"
        aria-label="Nouveau pseudo"
        value={name}
        onChange={(event) => {
          setName(event.target.value)
          setError(null)
        }}
        maxLength={40}
        autoFocus
      />
      <p className="muted">
        {MIN_LENGTH} à {MAX_LENGTH} caractères : lettres, chiffres, espaces et - _ . '
        {trimmed !== '' && ` (${length}/${MAX_LENGTH})`}
      </p>
      {error && (
        <p role="alert" className="login-error">
          {error}
        </p>
      )}
      <div className="login-actions">
        <button type="submit" className="button-primary" disabled={!lengthOk || busy}>
          Continuer
        </button>
      </div>
    </form>
  )
}
