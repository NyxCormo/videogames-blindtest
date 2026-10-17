import { useCurrentListener } from '../../context/CurrentListenerContext'
import './ListenerBanner.css'

export function ListenerBanner() {
  const { listener, disconnect, openLogin } = useCurrentListener()

  if (listener) {
    return (
      <div className="listener-banner">
        <span>Connecté : {listener.name}</span>
        <button type="button" onClick={disconnect}>Se déconnecter</button>
      </div>
    )
  }

  return (
    <div className="listener-banner">
      <button type="button" onClick={openLogin}>Se connecter</button>
    </div>
  )
}
