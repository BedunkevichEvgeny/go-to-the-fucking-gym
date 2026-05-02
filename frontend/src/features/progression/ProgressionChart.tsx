import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { ProgressionPoint } from '../../types/api';

/** Props for rendering exercise progression chart data. */
interface ProgressionChartProps {
  /** Ordered progression points returned by the backend progression endpoint. */
  points: ProgressionPoint[];
}

export function ProgressionChart({ points }: ProgressionChartProps) {
  if (points.length < 2) {
    return <p className="card">Not enough data to show trend.</p>;
  }

  return (
    <div className="card chart-card">
      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={points}>
          <XAxis dataKey="sessionDate" />
          <YAxis />
          <Tooltip />
          <Line dataKey="metricValue" stroke="#4f46e5" strokeWidth={2} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

