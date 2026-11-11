import { useState } from 'react'
import { Link } from 'react-router'
import { MenuIcon } from '../icons'
import { ListenerBanner } from '../ListenerBanner/ListenerBanner'
import { NotificationBanner } from '../NotificationBanner/NotificationBanner'
import { RoomBanner } from '../RoomBanner/RoomBanner'
import './SiteHeader.css'

const LINKS = [
  { to: '/', label: 'Accueil' },
  { to: '/tracks', label: 'Musiques' },
  { to: '/blindtests', label: 'Blindtests' },
  { to: '/rooms', label: 'Salles' },
  { to: '/discover', label: 'Découvrir' },
  { to: '/admin', label: 'Admin' },
]

export function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="site-header">
      <nav>
        <button
          type="button"
          className="nav-toggle"
          aria-expanded={menuOpen}
          aria-controls="nav-links"
          onClick={() => setMenuOpen(!menuOpen)}
        >
          <MenuIcon />
          Menu
        </button>
        <div id="nav-links" className={`nav-links${menuOpen ? ' nav-links-open' : ''}`}>
          {LINKS.map((link) => (
            <Link key={link.to} to={link.to} onClick={() => setMenuOpen(false)}>
              {link.label}
            </Link>
          ))}
          <a href="/api/export/sheet">Exporter en CSV</a>
        </div>
        <ListenerBanner />
      </nav>
      <NotificationBanner />
      <RoomBanner />
    </header>
  )
}
