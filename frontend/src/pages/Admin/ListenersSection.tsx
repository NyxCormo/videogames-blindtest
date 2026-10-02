import { useEffect, useState } from 'react'
import { AdminSessionExpired, deleteItem, fetchListenerUsage, mergeItem, type ListenerUsage } from '../../api/admin'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { useNotification } from '../../context/NotificationContext'
import { MergeForm } from './MergeForm'

type Props = {
  token: string
  onSessionExpired: () => void
}

function describe(listener: ListenerUsage): string {
  return `${listener.name} (${listener.votes} votes, ${listener.blindtests} blindtests)`
}

export function ListenersSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const { listener: current, connect, disconnect } = useCurrentListener()
  const [listeners, setListeners] = useState<ListenerUsage[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const selected = listeners.find((item) => item.id === selectedId) ?? null

  function load() {
    return fetchListenerUsage(token).then(setListeners)
  }

  function handleError(err: Error) {
    if (err instanceof AdminSessionExpired) {
      onSessionExpired()
    }
    notify(err.message, 'error')
  }

  useEffect(() => {
    load().catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function remove(target: ListenerUsage) {
    if (!window.confirm(`Supprimer le pseudo « ${target.name} » ? Ses ${target.votes} votes et ses scores dans ${target.blindtests} blindtests seront perdus.`)) return
    deleteItem(token, 'listeners', target.id)
      .then(() => {
        // Le navigateur qui était connecté avec ce pseudo ne doit pas continuer à l'utiliser.
        if (current?.id === target.id) disconnect()
        setSelectedId(null)
        notify(`Pseudo « ${target.name} » supprimé.`, 'success')
        return load()
      })
      .catch(handleError)
  }

  function merge(source: ListenerUsage, target: ListenerUsage) {
    if (
      !window.confirm(
        `Fusionner « ${source.name} » dans « ${target.name} » ? Ses votes et ses scores passeront à « ${target.name} », puis « ${source.name} » sera supprimé. En cas de doublon, ce sont ceux de « ${target.name} » qui restent.`,
      )
    )
      return
    mergeItem(token, 'listeners', source.id, target.id)
      .then(() => {
        if (current?.id === source.id) connect({ id: target.id, name: target.name })
        setSelectedId(target.id)
        notify(`« ${source.name} » fusionné dans « ${target.name} ».`, 'success')
        return load()
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Pseudos</h2>
      <NamePicker
        items={listeners}
        selected={selected}
        onSelect={(item) => setSelectedId(item.id)}
        onClear={() => setSelectedId(null)}
        placeholder="Chercher un pseudo"
        renderLabel={describe}
      />
      {selected && (
        <>
          <p className="admin-muted">{describe(selected)}</p>
          <MergeForm
            key={selected.id}
            items={listeners.filter((item) => item.id !== selected.id)}
            renderLabel={describe}
            onMerge={(target) => merge(selected, target)}
          />
          <div className="admin-row">
            <button type="button" onClick={() => remove(selected)}>
              Supprimer le pseudo
            </button>
          </div>
        </>
      )}
    </section>
  )
}
