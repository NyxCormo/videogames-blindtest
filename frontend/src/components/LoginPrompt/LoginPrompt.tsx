import { useCurrentListener } from '../../context/CurrentListenerContext'
import './LoginPrompt.css'

type Props = {
  message: string
}

export function LoginPrompt({ message }: Props) {
  const { openLogin } = useCurrentListener()

  return (
    <div className="login-prompt">
      <p>{message}</p>
      <button type="button" className="button button-primary button-large" onClick={openLogin}>
        Se connecter
      </button>
    </div>
  )
}
