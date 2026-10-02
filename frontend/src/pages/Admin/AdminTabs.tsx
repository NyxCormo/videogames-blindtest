export type AdminTab = { id: string; label: string }

type Props = {
  tabs: AdminTab[]
  active: string
  onChange: (id: string) => void
}

export function AdminTabs({ tabs, active, onChange }: Props) {
  return (
    <div className="admin-tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === active}
          className={tab.id === active ? 'admin-tab admin-tab-active' : 'admin-tab'}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
