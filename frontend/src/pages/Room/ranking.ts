import type { RoomPlayer } from '../../api/rooms'

export type RankedPlayer = RoomPlayer & { rank: number }

export function ranking(players: RoomPlayer[]): RankedPlayer[] {
  return [...players]
    .sort((a, b) => b.score - a.score)
    .map((player) => ({ ...player, rank: 1 + players.filter((other) => other.score > player.score).length }))
}

export function rankLabel(rank: number): string {
  return rank === 1 ? '1er' : `${rank}e`
}
