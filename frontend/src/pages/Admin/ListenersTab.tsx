import { useEffect, useState } from 'react'
import { fetchListenerUsage, type ListenerUsage } from '../../api/admin'
import { plural } from './adminText'
import { EntityList } from './EntityList'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function ListenersTab({ token, onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [listeners, setListeners] = useState<ListenerUsage[] | null>(null)

  useEffect(() => {
    fetchListenerUsage(token).then(setListeners).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <EntityList
      title="Pseudos"
      items={listeners}
      label={(listener) => listener.name}
      detail={(listener) => `${plural(listener.votes, 'vote', 'votes')} · ${plural(listener.blindtests, 'blindtest', 'blindtests')}`}
      emptyText="Aucun pseudo."
    />
  )
}
