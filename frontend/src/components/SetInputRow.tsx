import type { StrengthSetInput } from '../types/api';

interface SetInputRowProps {
  index: number;
  value: StrengthSetInput;
  onChange: (next: StrengthSetInput) => void;
  onRemove?: () => void;
}

export function SetInputRow({ index, value, onChange, onRemove }: SetInputRowProps) {
  return (
    <div className="row-grid card subtle">
      <label>
        <span>Set {index + 1} reps</span>
        <input
          min={1}
          type="number"
          value={value.reps}
          onChange={(event) => onChange({ ...value, reps: Number(event.target.value) })}
        />
      </label>
      <label>
        <span>Weight</span>
        <input
          min={0}
          step="0.5"
          type="number"
          value={value.weightValue ?? ''}
          disabled={value.isBodyWeight}
          onChange={(event) =>
            onChange({
              ...value,
              weightValue: event.target.value ? Number(event.target.value) : null,
            })
          }
        />
      </label>
      <label>
        <span>Unit</span>
        <select
          value={value.weightUnit ?? 'KG'}
          disabled={value.isBodyWeight}
          onChange={(event) => onChange({ ...value, weightUnit: event.target.value as 'KG' | 'LBS' })}
        >
          <option value="KG">KG</option>
          <option value="LBS">LBS</option>
        </select>
      </label>
      <label className="checkbox-field">
        <input
          checked={value.isBodyWeight}
          type="checkbox"
          onChange={(event) =>
            onChange({
              ...value,
              isBodyWeight: event.target.checked,
              weightValue: event.target.checked ? null : value.weightValue,
            })
          }
        />
        <span>Bodyweight</span>
      </label>
      {onRemove ? (
        <button className="button ghost" type="button" onClick={onRemove}>
          Remove set
        </button>
      ) : null}
    </div>
  );
}

