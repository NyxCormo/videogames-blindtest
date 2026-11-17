import './ChoiceGroup.css'

export type Choice<T extends string> = { value: T; label: string; hint: string }

type Props<T extends string> = {
  legend: string
  choices: Choice<T>[]
  value: T
  onChange: (value: T) => void
}

export function ChoiceGroup<T extends string>({ legend, choices, value, onChange }: Props<T>) {
  const selected = choices.find((choice) => choice.value === value)

  return (
    <fieldset className="choice-group">
      <legend>{legend}</legend>
      <div className="choice-group-buttons">
        {choices.map((choice) => (
          <button
            key={choice.value}
            type="button"
            aria-pressed={choice.value === value}
            onClick={() => onChange(choice.value)}
          >
            {choice.label}
          </button>
        ))}
      </div>
      {selected && <p className="choice-group-hint">{selected.hint}</p>}
    </fieldset>
  )
}
