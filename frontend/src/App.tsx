import { BrowserRouter, Link, Route, Routes } from 'react-router'
import './index.css'
import { ListenerBanner } from './components/ListenerBanner/ListenerBanner'
import { CurrentListenerProvider } from './context/CurrentListenerContext'
import { BlindtestCreatePage } from './pages/BlindtestCreate/BlindtestCreatePage'
import { BlindtestLeaderboardPage } from './pages/BlindtestLeaderboard/BlindtestLeaderboardPage'
import { BlindtestListPage } from './pages/BlindtestList/BlindtestListPage'
import { BlindtestPlayPage } from './pages/BlindtestPlay/BlindtestPlayPage'
import { DiscoverPage } from './pages/Discover/DiscoverPage'
import { EnrichmentPage } from './pages/Enrichment/EnrichmentPage'
import { KnowledgePage } from './pages/Knowledge/KnowledgePage'
import { KnowledgeListPage } from './pages/KnowledgeList/KnowledgeListPage'
import { TrackDetailPage } from './pages/TrackDetail/TrackDetailPage'
import { TrackListPage } from './pages/TrackList/TrackListPage'
import { TutorialPage } from './pages/Tutorial/TutorialPage'

function App() {
  return (
    <CurrentListenerProvider>
      <BrowserRouter>
        <nav>
          <div className="nav-links">
            <Link to="/">Musiques</Link>
            <Link to="/blindtests">Blindtests</Link>
            <Link to="/knowledge">Ma culture</Link>
            <Link to="/enrichment">Enrichissement</Link>
            <Link to="/tutorial">Tutoriel</Link>
            <a href="/api/export/sheet">Exporter en CSV</a>
          </div>
          <ListenerBanner />
        </nav>
        <main>
          <Routes>
            <Route path="/" element={<TrackListPage />} />
            <Route path="/tracks/:id" element={<TrackDetailPage />} />
            <Route path="/blindtests" element={<BlindtestListPage />} />
            <Route path="/blindtests/new" element={<BlindtestCreatePage />} />
            <Route path="/blindtests/:id/play" element={<BlindtestPlayPage />} />
            <Route path="/blindtests/:id/leaderboard" element={<BlindtestLeaderboardPage />} />
            <Route path="/knowledge" element={<KnowledgePage />} />
            <Route path="/knowledge/discover" element={<DiscoverPage />} />
            <Route path="/knowledge/list" element={<KnowledgeListPage />} />
            <Route path="/enrichment" element={<EnrichmentPage />} />
            <Route path="/tutorial" element={<TutorialPage />} />
          </Routes>
        </main>
      </BrowserRouter>
    </CurrentListenerProvider>
  )
}

export default App
