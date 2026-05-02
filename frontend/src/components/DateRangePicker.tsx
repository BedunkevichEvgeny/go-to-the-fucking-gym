/** Date range selection props for history filtering controls. */
interface DateRangePickerProps {
  /** Inclusive start date in yyyy-mm-dd format. */
  dateFrom: string;
  /** Inclusive end date in yyyy-mm-dd format. */
  dateTo: string;
  /** Emits the next date range whenever either input changes. */
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

