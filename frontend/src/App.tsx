import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router'
import './index.css'
import { ListenerBanner } from './components/ListenerBanner/ListenerBanner'
import { NotificationBanner } from './components/NotificationBanner/NotificationBanner'
import { CurrentListenerProvider } from './context/CurrentListenerContext'
import { NotificationProvider } from './context/NotificationContext'
import { AdminPage } from './pages/Admin/AdminPage'
import { BlindtestCreatePage } from './pages/BlindtestCreate/BlindtestCreatePage'
import { BlindtestLeaderboardPage } from './pages/BlindtestLeaderboard/BlindtestLeaderboardPage'
import { BlindtestListPage } from './pages/BlindtestList/BlindtestListPage'
import { BlindtestPlayPage } from './pages/BlindtestPlay/BlindtestPlayPage'
import { DiscoverPage } from './pages/Discover/DiscoverPage'
import { HomePage } from './pages/Home/HomePage'
import { TrackDetailPage } from './pages/TrackDetail/TrackDetailPage'
import { TracksPage } from './pages/Tracks/TracksPage'

function App() {
  return (
    <CurrentListenerProvider>
      <NotificationProvider>
      <BrowserRouter>
        <header className="site-header">
          <nav>
            <div className="nav-links">
              <Link to="/">Accueil</Link>
              <Link to="/tracks">Musiques</Link>
              <Link to="/blindtests">Blindtests</Link>
              <Link to="/discover">Découvrir</Link>
              <Link to="/admin">Admin</Link>
              <a href="/api/export/sheet">Exporter en CSV</a>
            </div>
            <ListenerBanner />
          </nav>
          <NotificationBanner />
        </header>
        <main>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/tracks" element={<TracksPage />} />
            <Route path="/tracks/:id" element={<TrackDetailPage />} />
            <Route path="/blindtests" element={<BlindtestListPage />} />
            <Route path="/blindtests/new" element={<BlindtestCreatePage />} />
            <Route path="/blindtests/:id/play" element={<BlindtestPlayPage />} />
            <Route path="/blindtests/:id/leaderboard" element={<BlindtestLeaderboardPage />} />
            <Route path="/discover" element={<DiscoverPage />} />
            <Route path="/knowledge" element={<Navigate to="/discover" replace />} />
            <Route path="/knowledge/discover" element={<Navigate to="/discover" replace />} />
            <Route path="/knowledge/list" element={<Navigate to="/tracks?tab=votes" replace />} />
            <Route path="/enrichment" element={<Navigate to="/tracks?tab=enrichir" replace />} />
            <Route path="/tutorial" element={<Navigate to="/" replace />} />
            <Route path="/admin" element={<AdminPage />} />
          </Routes>
        </main>
      </BrowserRouter>
      </NotificationProvider>
    </CurrentListenerProvider>
  )
}

export default App
