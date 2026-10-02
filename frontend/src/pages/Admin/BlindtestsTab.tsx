import { useEffect, useState } from 'react'
import { fetchBlindtests, type Blindtest } from '../../api/blindtests'
import { EntityList } from './EntityList'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  onSessionExpired: () => void
}

export function BlindtestsTab({ onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [blindtests, setBlindtests] = useState<Blindtest[] | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    fetchBlindtests(controller.signal)
      .then(setBlindtests)
      .catch((err: Error) => {
        if (!controller.signal.aborted) handleError(err)
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <EntityList
      title="Blindtests"
      items={blindtests}
      label={(blindtest) => blindtest.name}
      detail={(blindtest) => `créé le ${new Date(blindtest.createdAt).toLocaleDateString('fr-FR')}`}
      emptyText="Aucun blindtest."
      sorted={false}
    />
  )
}
