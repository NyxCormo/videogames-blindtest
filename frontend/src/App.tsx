import { BrowserRouter, Link, Route, Routes } from 'react-router'
import './index.css'
import { ListenerBanner } from './components/ListenerBanner/ListenerBanner'
import { CurrentListenerProvider } from './context/CurrentListenerContext'
import { BlindtestCreatePage } from './pages/BlindtestCreate/BlindtestCreatePage'
import { BlindtestListPage } from './pages/BlindtestList/BlindtestListPage'
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
            <Route path="/blindtests" element={<BlindtestListPage />} />
            <Route path="/blindtests/new" element={<BlindtestCreatePage />} />
          </Routes>
        </main>
      </BrowserRouter>
    </CurrentListenerProvider>
  )
}

export default App
