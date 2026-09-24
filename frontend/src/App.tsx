import { BrowserRouter, Link, Route, Routes } from 'react-router'
import './index.css'
import { ListenerBanner } from './components/ListenerBanner/ListenerBanner'
import { CurrentListenerProvider } from './context/CurrentListenerContext'
import { BlindtestCreatePage } from './pages/BlindtestCreate/BlindtestCreatePage'
import { BlindtestLeaderboardPage } from './pages/BlindtestLeaderboard/BlindtestLeaderboardPage'
import { BlindtestListPage } from './pages/BlindtestList/BlindtestListPage'
import { BlindtestPlayPage } from './pages/BlindtestPlay/BlindtestPlayPage'
import { TrackDetailPage } from './pages/TrackDetail/TrackDetailPage'
import { TrackListPage } from './pages/TrackList/TrackListPage'

function App() {
  return (
    <CurrentListenerProvider>
      <BrowserRouter>
        <nav>
          <div className="nav-links">
            <Link to="/">Musiques</Link>
            <Link to="/blindtests">Blindtests</Link>
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
          </Routes>
        </main>
      </BrowserRouter>
    </CurrentListenerProvider>
  )
}

export default App
