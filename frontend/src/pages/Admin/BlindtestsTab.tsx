import { useEffect, useState } from 'react'
import { fetchBlindtests, type Blindtest } from '../../api/blindtests'
import { EntityList } from './EntityList'
import { useMergeSelection } from './mergeSelection'
import { useAdminErrorHandler } from './useAdminErrorHandler'
import { useEntityActions } from './useEntityActions'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function BlindtestsTab({ token, onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [blindtests, setBlindtests] = useState<Blindtest[] | null>(null)

  function load(signal?: AbortSignal) {
    return fetchBlindtests(signal).then(setBlindtests)
  }

  const { lastMerge } = useMergeSelection()

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal).catch((err: Error) => {
      if (!controller.signal.aborted) handleError(err)
    })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lastMerge])

  const actions = useEntityActions<Blindtest>({ token, kind: 'blindtests', noun: 'le blindtest', onSessionExpired, reload: load })

  return (
    <>
      {actions.modal}
      <EntityList
        title="Blindtests"
        items={blindtests}
        label={(blindtest) => blindtest.name}
        detail={(blindtest) => `créé le ${new Date(blindtest.createdAt).toLocaleDateString('fr-FR')}`}
        onRename={actions.rename}
        onDelete={actions.askDelete}
        emptyText="Aucun blindtest."
        sorted={false}
      />
    </>
  )
}
