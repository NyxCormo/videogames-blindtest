import { useSearchParams } from 'react-router'
import { Tabs } from '../../components/Tabs/Tabs'
import { EnrichTab } from './Enrich/EnrichTab'
import { ListenTab } from './Listen/ListenTab'
import { VotesTab } from './Votes/VotesTab'
import './TracksPage.css'

const TABS = [
  { id: 'ecouter', label: 'Écouter' },
  { id: 'enrichir', label: 'Enrichir' },
  { id: 'votes', label: 'Mes votes' },
]

export function TracksPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = TABS.some((item) => item.id === searchParams.get('tab')) ? searchParams.get('tab')! : TABS[0].id

  return (
    <>
      <h1>Musiques</h1>
      <Tabs tabs={TABS} active={tab} onChange={(id) => setSearchParams(id === TABS[0].id ? {} : { tab: id })} />
      {tab === 'ecouter' && <ListenTab />}
      {tab === 'enrichir' && <EnrichTab />}
      {tab === 'votes' && <VotesTab />}
    </>
  )
}
