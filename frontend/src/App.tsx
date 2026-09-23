import { BrowserRouter, Link, Route, Routes } from 'react-router'
import './index.css'
import { BlindtestCreatePage } from './pages/BlindtestCreate/BlindtestCreatePage'
import { BlindtestListPage } from './pages/BlindtestList/BlindtestListPage'
import { TrackListPage } from './pages/TrackList/TrackListPage'

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/">Musiques</Link>
        <Link to="/blindtests">Blindtests</Link>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<TrackListPage />} />
          <Route path="/blindtests" element={<BlindtestListPage />} />
          <Route path="/blindtests/new" element={<BlindtestCreatePage />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

export default App
