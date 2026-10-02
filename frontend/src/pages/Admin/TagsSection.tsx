import { useEffect, useState } from 'react'
import { AdminSessionExpired, deleteItem, fetchTagCounts, mergeItem, renameItem, type TagCount } from '../../api/admin'
import { NamePicker } from '../../components/NamePicker/NamePicker'
import { useNotification } from '../../context/NotificationContext'
import { MergeForm } from './MergeForm'
import { RenameForm } from './RenameForm'

type Props = {
  token: string
  onSessionExpired: () => void
}

function describe(tag: TagCount): string {
  return `${tag.name} (${tag.typeName}, ${tag.tracks} musiques)`
}

export function TagsSection({ token, onSessionExpired }: Props) {
  const { notify } = useNotification()
  const [tags, setTags] = useState<TagCount[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const selected = tags.find((tag) => tag.id === selectedId) ?? null

  function load() {
    return fetchTagCounts(token).then(setTags)
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

  function rename(tag: TagCount, name: string) {
    renameItem(token, 'tags', tag.id, name)
      .then((updated) => {
        notify(`Tag « ${tag.name} » renommé en « ${updated.name} ».`, 'success')
        return load()
      })
      .catch(handleError)
  }

  function merge(source: TagCount, target: TagCount) {
    if (
      !window.confirm(
        `Fusionner le tag « ${source.name} » (${source.typeName}) dans « ${target.name} » (${target.typeName}) ? Ses ${source.tracks} musiques prendront « ${target.name} », puis « ${source.name} » sera supprimé.`,
      )
    )
      return
    mergeItem(token, 'tags', source.id, target.id)
      .then(() => {
        setSelectedId(target.id)
        notify(`Tag « ${source.name} » fusionné dans « ${target.name} ».`, 'success')
        return load()
      })
      .catch(handleError)
  }

  function remove(tag: TagCount) {
    if (!window.confirm(`Supprimer le tag « ${tag.name} » ? Il sera retiré de ses ${tag.tracks} musiques.`)) return
    deleteItem(token, 'tags', tag.id)
      .then(() => {
        setSelectedId(null)
        notify(`Tag « ${tag.name} » supprimé.`, 'success')
        return load()
      })
      .catch(handleError)
  }

  return (
    <section className="admin-section">
      <h2>Tags</h2>
      <NamePicker
        items={tags}
        selected={selected}
        onSelect={(tag) => setSelectedId(tag.id)}
        onClear={() => setSelectedId(null)}
        placeholder="Chercher un tag"
        renderLabel={describe}
        getSearchText={(tag) => `${tag.name} ${tag.typeName}`}
      />
      {selected && (
        <>
          <p className="admin-muted">{describe(selected)}</p>
          <RenameForm
            key={`${selected.id}-${selected.name}`}
            current={selected.name}
            onRename={(name) => rename(selected, name)}
          />
          <MergeForm
            key={`merge-${selected.id}`}
            items={tags.filter((tag) => tag.id !== selected.id)}
            renderLabel={describe}
            getSearchText={(tag) => `${tag.name} ${tag.typeName}`}
            onMerge={(target) => merge(selected, target)}
          />
          <div className="admin-row">
            <button type="button" onClick={() => remove(selected)}>
              Supprimer le tag
            </button>
          </div>
        </>
      )}
    </section>
  )
}
