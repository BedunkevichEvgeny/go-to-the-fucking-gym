import type { CardioLapInput } from '../types/api';

/** Props for editing a single cardio lap row. */
interface CardioLapInputRowProps {
  /** Zero-based lap index used for UI labels. */
  index: number;
  /** Current lap value bound to input controls. */
  value: CardioLapInput;
  /** Called with updated lap values after user edits. */
  onChange: (next: CardioLapInput) => void;
  /** Optional callback to remove the current lap. */
  onRemove?: () => void;
}

export function CardioLapInputRow({ index, value, onChange, onRemove }: CardioLapInputRowProps) {
  return (
    <div className="row-grid card subtle">
      <label>
        <span>Lap {index + 1} duration (sec)</span>
        <input
          min={1}
          type="number"
          value={value.durationSeconds}
          onChange={(event) => onChange({ ...value, durationSeconds: Number(event.target.value) })}
        />
      </label>
      <label>
        <span>Distance</span>
        <input
          min={0}
          step="0.1"
          type="number"
          value={value.distanceValue ?? ''}
          onChange={(event) =>
            onChange({
              ...value,
              distanceValue: event.target.value ? Number(event.target.value) : null,
            })
          }
        />
      </label>
      <label>
        <span>Unit</span>
        <select
          value={value.distanceUnit ?? 'KM'}
          onChange={(event) => onChange({ ...value, distanceUnit: event.target.value as 'KM' | 'MI' })}
        >
          <option value="KM">KM</option>
          <option value="MI">MI</option>
        </select>
      </label>
      {onRemove ? (
        <button className="button ghost" type="button" onClick={onRemove}>
          Remove lap
        </button>
      ) : null}
    </div>
  );
}

