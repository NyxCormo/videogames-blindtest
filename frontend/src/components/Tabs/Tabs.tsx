import { Tabs as BaseTabs } from '@base-ui/react/tabs'
import './Tabs.css'

export type Tab = { id: string; label: string }

type Props = {
  tabs: Tab[]
  active: string
  onChange: (id: string) => void
}

export function Tabs({ tabs, active, onChange }: Props) {
  return (
    <BaseTabs.Root value={active} onValueChange={(value) => onChange(String(value))}>
      <BaseTabs.List className="tabs">
        {tabs.map((tab) => (
          <BaseTabs.Tab key={tab.id} value={tab.id} className="tab">
            {tab.label}
          </BaseTabs.Tab>
        ))}
      </BaseTabs.List>
    </BaseTabs.Root>
  )
}
