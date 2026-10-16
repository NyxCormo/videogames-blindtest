type Props = {
  franchise: string | null
  game: string | null
  track: string | null
}

export function TrackLabel({ franchise, game, track }: Props) {
  return (
    <>
      <strong>{franchise}</strong> — {game} — {track}
    </>
  )
}
