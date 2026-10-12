import { Select as BaseSelect } from '@base-ui/react/select'
import './Select.css'

export type SelectOption<T extends string> = { value: T; label: string }

type Props<T extends string> = {
  options: SelectOption<T>[]
  value: T
  onChange: (value: T) => void
  ariaLabel?: string
}

export function Select<T extends string>({ options, value, onChange, ariaLabel }: Props<T>) {
  return (
    <BaseSelect.Root
      items={options}
      value={value}
      onValueChange={(next) => {
        if (next !== null) onChange(next)
      }}
    >
      <BaseSelect.Trigger className="select-trigger" aria-label={ariaLabel}>
        <BaseSelect.Value />
        <BaseSelect.Icon className="select-icon">▾</BaseSelect.Icon>
      </BaseSelect.Trigger>
      <BaseSelect.Portal>
        <BaseSelect.Positioner align="start" sideOffset={4} alignItemWithTrigger={false}>
          <BaseSelect.Popup className="select-popup">
            {options.map((option) => (
              <BaseSelect.Item key={option.value} value={option.value} className="select-item">
                <BaseSelect.ItemText>{option.label}</BaseSelect.ItemText>
                <BaseSelect.ItemIndicator className="select-item-indicator">✓</BaseSelect.ItemIndicator>
              </BaseSelect.Item>
            ))}
          </BaseSelect.Popup>
        </BaseSelect.Positioner>
      </BaseSelect.Portal>
    </BaseSelect.Root>
  )
}
