interface DateRangePickerProps {
  dateFrom: string;
  dateTo: string;
  onChange: (next: { dateFrom: string; dateTo: string }) => void;
}

export function DateRangePicker({ dateFrom, dateTo, onChange }: DateRangePickerProps) {
  return (
    <div className="inline-grid">
      <label>
        <span>From</span>
        <input
          type="date"
          value={dateFrom}
          onChange={(event) => onChange({ dateFrom: event.target.value, dateTo })}
        />
      </label>
      <label>
        <span>To</span>
        <input
          type="date"
          value={dateTo}
          onChange={(event) => onChange({ dateFrom, dateTo: event.target.value })}
        />
      </label>
    </div>
  );
}

