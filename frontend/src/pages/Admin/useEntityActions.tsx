import { useState, type ReactNode } from 'react'
import { deleteItem, fetchDeletePreview, renameItem, type AdminKind, type DeletePreview } from '../../api/admin'
import { useNotification } from '../../context/NotificationContext'
import { ConfirmModal } from './ConfirmModal'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Named = { id: number; name: string }

type Options<T extends Named> = {
  token: string
  kind: AdminKind
  // pour les messages : « la franchise », « le jeu »…
  noun: string
  onSessionExpired: () => void
  reload: () => Promise<unknown>
  afterRename?: (item: T, name: string) => void
  afterDelete?: (item: T) => void
}

type PendingDelete<T> = { item: T; preview: DeletePreview | null }

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

// Renommer et supprimer marchent de la même façon pour tous les types : seuls la route et les textes changent.
export function useEntityActions<T extends Named>({
  token,
  kind,
  noun,
  onSessionExpired,
  reload,
  afterRename,
  afterDelete,
}: Options<T>): { rename: (item: T, name: string) => Promise<void>; askDelete: (item: T) => void; modal: ReactNode } {
  const { notify } = useNotification()
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [pending, setPending] = useState<PendingDelete<T> | null>(null)
  const [busy, setBusy] = useState(false)

  function rename(item: T, name: string): Promise<void> {
    return renameItem(token, kind, item.id, name)
      .then((updated) => {
        notify(`${capitalize(noun)} « ${item.name} » s'appelle maintenant « ${updated.name} ».`, 'success')
        afterRename?.(item, updated.name)
        return reload()
      })
      .then(() => undefined)
      .catch((err: Error) => {
        handleError(err)
        // La ligne reste en édition pour pouvoir corriger.
        throw err
      })
  }

  function askDelete(item: T) {
    setPending({ item, preview: null })
    fetchDeletePreview(token, kind, item.id)
      .then((preview) => setPending({ item, preview }))
      .catch((err: Error) => {
        setPending(null)
        handleError(err)
      })
  }

  function confirmDelete() {
    if (pending === null) return
    const { item } = pending
    setBusy(true)
    deleteItem(token, kind, item.id)
      .then(() => {
        notify(`Suppression de ${noun} « ${item.name} » terminée.`, 'success')
        afterDelete?.(item)
        setPending(null)
        return reload()
      })
      .catch(handleError)
      .finally(() => setBusy(false))
  }

  const modal =
    pending === null ? null : (
      <ConfirmModal
        title={`Supprimer ${noun} « ${pending.item.name} » ?`}
        confirmLabel="Supprimer"
        onConfirm={pending.preview?.allowed ? confirmDelete : null}
        onCancel={() => setPending(null)}
        busy={busy}
      >
        {pending.preview === null ? (
          <p>Calcul de ce qui sera supprimé…</p>
        ) : pending.preview.allowed ? (
          <p>{pending.preview.impact}</p>
        ) : (
          <p className="admin-modal-blocked">Suppression impossible. {pending.preview.impact}</p>
        )}
      </ConfirmModal>
    )

  return { rename, askDelete, modal }
}
