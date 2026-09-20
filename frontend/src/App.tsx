import { useEffect, useState } from 'react'

type Health = { status: string }

function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => res.json())
      .then(setHealth)
      .catch(() => setError(true))
  }, [])

  return (
    <main>
      <h1>Music Blindtest</h1>
      {health && <p>Backend : {health.status}</p>}
      {error && <p>Backend injoignable</p>}
    </main>
  )
}

export default App
