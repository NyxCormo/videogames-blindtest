import { useEffect, useState } from 'react'
import { fetchTagCounts, type TagCount } from '../../api/admin'
import { plural } from './adminText'
import { EntityList } from './EntityList'
import { useAdminErrorHandler } from './useAdminErrorHandler'
import { useEntityActions } from './useEntityActions'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function TagsTab({ token, onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [tags, setTags] = useState<TagCount[] | null>(null)

  function load() {
    return fetchTagCounts(token).then(setTags)
  }

  useEffect(() => {
    load().catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const actions = useEntityActions<TagCount>({ token, kind: 'tags', noun: 'le tag', onSessionExpired, reload: load })

  return (
    <>
      {actions.modal}
      <EntityList
        title="Tags"
        items={tags}
        label={(tag) => tag.name}
        detail={(tag) => `${tag.typeName} · ${plural(tag.tracks, 'musique', 'musiques')}`}
        searchText={(tag) => `${tag.name} ${tag.typeName}`}
        onRename={actions.rename}
        onDelete={actions.askDelete}
        emptyText="Aucun tag."
      />
    </>
  )
}
