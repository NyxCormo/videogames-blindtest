import { Dialog } from '@base-ui/react/dialog'
import { useState } from 'react'
import type { Listener } from '../../api/listeners'
import { useCurrentListener } from '../../context/CurrentListenerContext'
import { Tabs } from '../Tabs/Tabs'
import { ConnectPanel } from './ConnectPanel'
import { CreatePanel } from './CreatePanel'
import './LoginDialog.css'

const TABS = [
  { id: 'connect', label: "J'ai déjà un pseudo" },
  { id: 'create', label: 'Créer un pseudo' },
]

export function LoginDialog() {
  const { loginOpen, openLogin, closeLogin } = useCurrentListener()

  return (
    <Dialog.Root open={loginOpen} onOpenChange={(open) => (open ? openLogin() : closeLogin())}>
      <Dialog.Portal>
        <Dialog.Backdrop className="dialog-backdrop" />
        <Dialog.Popup className="dialog login-dialog">
          <div className="dialog-header">
            <Dialog.Title className="dialog-title">Se connecter</Dialog.Title>
            <Dialog.Close className="dialog-close" aria-label="Fermer">
              ×
            </Dialog.Close>
          </div>
          <LoginContent />
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

function LoginContent() {
  const { connect, closeLogin } = useCurrentListener()
  const [mode, setMode] = useState('connect')
  const [draft, setDraft] = useState('')

  function choose(listener: Listener) {
    connect(listener)
    closeLogin()
  }

  return (
    <>
      <Tabs tabs={TABS} active={mode} onChange={setMode} />
      {mode === 'connect' ? (
        <ConnectPanel
          onChoose={choose}
          onCreate={(name) => {
            setDraft(name)
            setMode('create')
          }}
        />
      ) : (
        <CreatePanel initialName={draft} onDone={choose} />
      )}
    </>
  )
}
