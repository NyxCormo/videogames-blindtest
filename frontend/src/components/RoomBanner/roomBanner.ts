import type { Room } from '../../api/rooms'
import { PHASE_LABELS } from '../../pages/Room/roomLabels'
import { plural } from '../../plural'

export type RoomBannerContent = {
  text: string
  link: string
  linkLabel: string
  codes: string[]
}

export function roomBanner(rooms: Room[], dismissed: Set<string>): RoomBannerContent | null {
  const visible = rooms.filter((room) => !dismissed.has(room.code))
  const codes = visible.map((room) => room.code)
  if (visible.length === 0) {
    return null
  }
  if (visible.length === 1) {
    const room = visible[0]
    return {
      text: `Salle publique ouverte : ${room.blindtestName} (${PHASE_LABELS[room.phase].toLowerCase()}, ${plural(room.players.length, 'joueur', 'joueurs')})`,
      link: `/rooms/${room.code}`,
      linkLabel: 'Rejoindre',
      codes,
    }
  }
  return {
    text: `${visible.length} salles publiques ouvertes`,
    link: '/rooms',
    linkLabel: 'Voir les salles',
    codes,
  }
}
