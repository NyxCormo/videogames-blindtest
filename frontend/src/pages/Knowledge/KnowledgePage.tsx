import { Link } from 'react-router'
import './KnowledgePage.css'

export function KnowledgePage() {
  return (
    <>
      <h1>Ma culture</h1>
      <p>Découvre les musiques que tu ne connais pas encore, ou complète directement la liste à la main.</p>
      <div className="knowledge-home-links">
        <Link to="/knowledge/discover">Découvrir</Link>
        <Link to="/knowledge/list">Liste des musiques</Link>
      </div>
    </>
  )
}
