import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router'
import { fetchPublicRooms, type Room } from '../../api/rooms'
import { UsersIcon } from '../icons'
import { roomBanner } from './bannerContent'
import './RoomBanner.css'

const REFRESH_MS = 15_000

export function RoomBanner() {
  const { pathname } = useLocation()
  const [rooms, setRooms] = useState<Room[]>([])
  const [dismissed, setDismissed] = useState<Set<string>>(new Set())

  useEffect(() => {
    const controller = new AbortController()
    function load() {
      fetchPublicRooms(controller.signal)
        .then(setRooms)
        .catch(() => {})
    }
    load()
    const interval = setInterval(load, REFRESH_MS)
    return () => {
      clearInterval(interval)
      controller.abort()
    }
  }, [])

  const banner = roomBanner(rooms, dismissed)
  if (!banner || pathname.startsWith('/rooms')) {
    return null
  }

  return (
    <div className="room-banner" role="status">
      <UsersIcon />
      <span>{banner.text}</span>
      <Link to={banner.link}>{banner.linkLabel}</Link>
      <button type="button" onClick={() => setDismissed(new Set([...dismissed, ...banner.codes]))}>
        Masquer
      </button>
    </div>
  )
}
