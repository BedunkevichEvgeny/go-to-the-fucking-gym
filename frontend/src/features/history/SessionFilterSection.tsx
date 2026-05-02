import { DateRangePicker } from '../../components/DateRangePicker';

/** Props for history list filter controls. */
interface SessionFilterSectionProps {
  /** Current inclusive start date filter. */
  dateFrom: string;
  /** Current inclusive end date filter. */
  dateTo: string;
  /** Current exercise-name search term filter. */
  exerciseName: string;
  /** Called when any filter field changes. */
  onChange: (next: { dateFrom: string; dateTo: string; exerciseName: string }) => void;
  /** Resets all filters to their default values. */
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

