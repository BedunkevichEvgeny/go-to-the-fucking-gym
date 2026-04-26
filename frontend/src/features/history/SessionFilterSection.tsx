import { DateRangePicker } from '../../components/DateRangePicker';

interface SessionFilterSectionProps {
  dateFrom: string;
  dateTo: string;
  exerciseName: string;
  onChange: (next: { dateFrom: string; dateTo: string; exerciseName: string }) => void;
  onClear: () => void;
}

export function SessionFilterSection({ dateFrom, dateTo, exerciseName, onChange, onClear }: SessionFilterSectionProps) {
  return (
    <section className="card stack-sm">
      <h2>Filters</h2>
      <DateRangePicker dateFrom={dateFrom} dateTo={dateTo} onChange={(dates) => onChange({ ...dates, exerciseName })} />
      <label>
        <span>Exercise name</span>
        <input value={exerciseName} onChange={(event) => onChange({ dateFrom, dateTo, exerciseName: event.target.value })} />
      </label>
      <div className="inline-actions">
        <button className="button ghost" type="button" onClick={onClear}>Clear</button>
      </div>
    </section>
  );
}

