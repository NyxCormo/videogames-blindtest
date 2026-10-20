import { useState } from 'react'
import { thumbnailOf } from '../../coverThumbnail'

export function CoverThumbnail({ url, alt }: { url: string; alt: string }) {
  const [full, setFull] = useState(false)
  return <img src={full ? url : thumbnailOf(url)} alt={alt} loading="lazy" onError={() => setFull(true)} />
}
