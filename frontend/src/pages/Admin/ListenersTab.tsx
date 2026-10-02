import { useEffect, useState } from 'react'
import { fetchListenerUsage, type ListenerUsage } from '../../api/admin'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { plural } from './adminText'
import { EntityList } from './EntityList'
import { useMergeSelection } from './mergeSelection'
import { useAdminErrorHandler } from './useAdminErrorHandler'
import { useEntityActions } from './useEntityActions'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function ListenersTab({ token, onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [listeners, setListeners] = useState<ListenerUsage[] | null>(null)

  const { listener: current, connect, disconnect } = useCurrentListener()

  function load() {
    return fetchListenerUsage(token).then(setListeners)
  }

  const { lastMerge } = useMergeSelection()

  useEffect(() => {
    load().catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lastMerge])

  // Le navigateur connecté avec ce pseudo doit suivre le changement.
  const actions = useEntityActions<ListenerUsage>({
    token,
    kind: 'listeners',
    noun: 'le pseudo',
    onSessionExpired,
    reload: load,
    afterRename: (item, name) => current?.id === item.id && connect({ id: item.id, name }),
    afterDelete: (item) => current?.id === item.id && disconnect(),
  })

  return (
    <>
      {actions.modal}
      <EntityList
        title="Pseudos"
        items={listeners}
        label={(listener) => listener.name}
        detail={(listener) => `${plural(listener.votes, 'vote', 'votes')} · ${plural(listener.blindtests, 'blindtest', 'blindtests')}`}
        onRename={actions.rename}
        onDelete={actions.askDelete}
        mergeKind="listeners"
        emptyText="Aucun pseudo."
      />
    </>
  )
}
