import { Accordion } from '@base-ui/react/accordion'
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { fetchListenerStats, fetchStats, type ListenerStats, type Stats } from '../../api/stats'
import { Stat } from '../../components/Stat/Stat'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { TUTORIAL_SECTIONS } from './tutorialSections'
import './HomePage.css'

function format(value: number): string {
  return value.toLocaleString('fr-FR').replace(/ /g, ' ')
}

export function HomePage() {
  const { listener, openLogin } = useCurrentListener()
  const [stats, setStats] = useState<Stats | null>(null)
  const [loaded, setLoaded] = useState<{ listenerId: number; stats: ListenerStats } | null>(null)
  const listenerId = listener?.id
  const mine = loaded && loaded.listenerId === listenerId ? loaded.stats : null

  useEffect(() => {
    const controller = new AbortController()
    fetchStats(controller.signal)
      .then(setStats)
      .catch(() => {})
    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (listenerId === undefined) return
    const controller = new AbortController()
    fetchListenerStats(listenerId, controller.signal)
      .then((value) => setLoaded({ listenerId, stats: value }))
      .catch(() => {})
    return () => controller.abort()
  }, [listenerId])

  const playablePercent = stats && stats.tracks > 0 ? Math.round((stats.playableTracks * 100) / stats.tracks) : 0

  return (
    <div className="home">
      <header className="home-hero">
        <h1>Music Blindtest</h1>
        <p>Les musiques de jeux vidéo d'InsaLan, à écouter, à deviner et à compléter.</p>
        <div className="home-actions">
          <Link className="button button-primary" to="/blindtests">
            Jouer à un blindtest
          </Link>
          <Link className="button" to="/discover">
            Découvrir des musiques
          </Link>
        </div>
      </header>

      {stats && (
        <section className="home-stats" aria-label="La base en chiffres">
          <div className="home-stats-group">
            <Stat value={format(stats.tracks)} label="Musiques" />
            <Stat value={format(stats.games)} label="Jeux" />
            <Stat value={format(stats.franchises)} label="Franchises" />
          </div>
          <div className="home-stats-group">
            <Stat value={`${playablePercent} %`} label="Musiques jouables" />
          </div>
          <div className="home-stats-group">
            <Stat value={format(stats.listeners)} label="Joueurs" />
            <Stat value={format(stats.votes)} label="Votes" />
          </div>
        </section>
      )}

      <section className="home-mine" aria-label="Vos chiffres">
        {!listener && (
          <button type="button" className="button button-primary button-large" onClick={openLogin}>
            Se connecter
          </button>
        )}
        {listener && mine && (
          <div className="home-stats-group">
            <Stat value={format(mine.votes)} label="Musiques votées" />
            <Stat value={format(mine.known)} label="Musiques connues" />
            <Stat value={format(mine.remaining)} label="À découvrir" />
          </div>
        )}
      </section>

      <Accordion.Root className="home-accordion" multiple>
        {TUTORIAL_SECTIONS.map((section) => (
          <Accordion.Item key={section.title} className="home-accordion-item">
            <Accordion.Header className="home-accordion-header">
              <Accordion.Trigger className="home-accordion-trigger">
                {section.title}
                <span className="home-accordion-icon" aria-hidden="true">
                  ▾
                </span>
              </Accordion.Trigger>
            </Accordion.Header>
            <Accordion.Panel className="home-accordion-panel">
              <div className="home-accordion-content">{section.content}</div>
            </Accordion.Panel>
          </Accordion.Item>
        ))}
      </Accordion.Root>
    </div>
  )
}
