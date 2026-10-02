import { useEffect, useState } from 'react'
import { fetchTagCounts, type TagCount } from '../../api/admin'
import { plural } from './adminText'
import { EntityList } from './EntityList'
import { useAdminErrorHandler } from './useAdminErrorHandler'

type Props = {
  token: string
  onSessionExpired: () => void
}

export function TagsTab({ token, onSessionExpired }: Props) {
  const handleError = useAdminErrorHandler(onSessionExpired)
  const [tags, setTags] = useState<TagCount[] | null>(null)

  useEffect(() => {
    fetchTagCounts(token).then(setTags).catch(handleError)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <EntityList
      title="Tags"
      items={tags}
      label={(tag) => tag.name}
      detail={(tag) => `${tag.typeName} · ${plural(tag.tracks, 'musique', 'musiques')}`}
      searchText={(tag) => `${tag.name} ${tag.typeName}`}
      emptyText="Aucun tag."
    />
  )
}
