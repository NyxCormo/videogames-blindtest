import { useState } from 'react'
import { fetchMergePreview, mergeItem, type AdminKind, type MergePreview } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'
import { ConfirmModal } from './ConfirmModal'
import { useMergeSelection, type MergeItem, type MergeSlot } from './mergeSelection'
import { useAdminErrorHandler } from './useAdminErrorHandler'

const KIND_LABELS: Partial<Record<AdminKind, string>> = {
  franchises: 'Franchise',
  games: 'Jeu',
  tracks: 'Musique',
  listeners: 'Pseudo',
  tags: 'Tag',
}

type Props = {
  token: string
  onSessionExpired: () => void
}

type Pending = { source: MergeItem; target: MergeItem; preview: MergePreview | null }

export function MergeFooter({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const { selection, remove, switchKeep, clear, merged } = useMergeSelection()
  const [pending, setPending] = useState<Pending | null>(null)
  const [busy, setBusy] = useState(false)

  if (selection === null) return null
  const { kind, a, b, keep } = selection
  const kept = keep === 'a' ? a : b
  const complete = a !== null && b !== null

  function slot(item: MergeItem | null, name: MergeSlot) {
    return item === null ? (
      <span className="merge-slot merge-slot-empty">Choisir un 2ᵉ élément</span>
    ) : (
      <span className={`merge-slot merge-slot-${name}`}>
        {item.name}
        <button type="button" aria-label={`Retirer ${item.name} de la fusion`} onClick={() => remove(name)}>
          ×
        </button>
      </span>
    )
  }

  function askConfirmation() {
    if (a === null || b === null) return
    const target = keep === 'a' ? a : b
    const source = keep === 'a' ? b : a
    setPending({ source, target, preview: null })
    fetchMergePreview(token, kind, source.id, target.id)
      .then((preview) => setPending({ source, target, preview }))
      .catch((err: Error) => {
        setPending(null)
        handleError(err)
      })
  }

  function confirmMerge() {
    if (pending === null) return
    const { source, target } = pending
    setBusy(true)
    mergeItem(token, kind, source.id, target.id)
      .then(() => {
        notify(`Fusion terminée : « ${target.name} » remplace « ${source.name} ».`, 'success')
        setPending(null)
        merged({ kind, sourceId: source.id, targetId: target.id })
      })
      .catch(handleError)
      .finally(() => setBusy(false))
  }

  return (
    <>
      <div className="merge-footer" role="region" aria-label="Fusion en cours">
        <span className="merge-kind">{KIND_LABELS[kind]}</span>
        {slot(a, 'a')}
        <span className="merge-operator">+</span>
        {slot(b, 'b')}
        <span className="merge-operator">=</span>
        <button
          type="button"
          className={`merge-slot merge-result merge-slot-${keep}`}
          onClick={switchKeep}
          disabled={!complete}
          title="Cliquer pour changer l'élément gardé"
        >
          {kept?.name ?? '…'}
        </button>
        <span className="merge-actions">
          <button type="button" onClick={clear}>
            Annuler
          </button>
          <button type="button" className="merge-confirm" onClick={askConfirmation} disabled={!complete}>
            Confirmer la fusion
          </button>
        </span>
      </div>
      {pending !== null && (
        <ConfirmModal
          title={`Fusionner « ${pending.source.name} » dans « ${pending.target.name} » ?`}
          confirmLabel="Fusionner"
          onConfirm={pending.preview?.allowed ? confirmMerge : null}
          onCancel={() => setPending(null)}
          busy={busy}
        >
          {pending.preview === null ? (
            <p>Calcul de ce qui sera déplacé…</p>
          ) : pending.preview.allowed ? (
            <p>{pending.preview.summary}</p>
          ) : (
            <p className="admin-modal-blocked">Fusion impossible. {pending.preview.summary}</p>
          )}
        </ConfirmModal>
      )}
    </>
  )
}
