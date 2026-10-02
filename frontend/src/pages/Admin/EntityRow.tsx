type Props = {
  label: string
  detail?: string
  active?: boolean
  onOpen?: () => void
}

export function EntityRow({ label, detail, active = false, onOpen }: Props) {
  const content = (
    <>
      <span className="entity-row-label">{label}</span>
      {detail && <span className="entity-row-detail">{detail}</span>}
    </>
  )

  return (
    <li className={active ? 'entity-row entity-row-active' : 'entity-row'}>
      {onOpen ? (
        <button type="button" className="entity-row-main" aria-pressed={active} onClick={onOpen}>
          {content}
        </button>
      ) : (
        <div className="entity-row-main">{content}</div>
      )}
    </li>
  )
}
