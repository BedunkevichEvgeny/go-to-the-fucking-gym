import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { ProgressionChart } from '../ProgressionChart';

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div data-testid="responsive-container">{children}</div>,
  LineChart: ({ data, children }: { data: Array<{ sessionDate: string; metricValue: number }>; children: ReactNode }) => (
    <div data-testid="line-chart">
      {data.map((point) => (
        <span key={`${point.sessionDate}-${point.metricValue}`}>{point.sessionDate}</span>
      ))}
      {children}
    </div>
  ),
  XAxis: ({ dataKey }: { dataKey: string }) => <span data-testid="x-axis">{dataKey}</span>,
  YAxis: () => <span data-testid="y-axis">metricValue</span>,
  Tooltip: () => <span data-testid="tooltip">tooltip</span>,
  Line: ({ dataKey }: { dataKey: string }) => <span data-testid="line">{dataKey}</span>,
}));

describe('ProgressionChart', () => {
  it('renders progression chart points in chronological order for trend visualization', () => {
    render(
      <ProgressionChart
        points={[
          { sessionId: '1', sessionDate: '2026-04-01', metricType: 'WEIGHT', metricValue: 100 },
          { sessionId: '2', sessionDate: '2026-04-15', metricType: 'WEIGHT', metricValue: 110 },
        ]}
      />,
    );

    expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    expect(screen.getByText('2026-04-01')).toBeInTheDocument();
    expect(screen.getByText('2026-04-15')).toBeInTheDocument();
    expect(screen.getByTestId('x-axis')).toHaveTextContent('sessionDate');
    expect(screen.getByTestId('y-axis')).toHaveTextContent('metricValue');
    expect(screen.getByTestId('line')).toHaveTextContent('metricValue');
    expect(screen.getByTestId('tooltip')).toBeInTheDocument();
  });

  it('shows not-enough-data message when only one progression point exists', () => {
    render(
      <ProgressionChart
        points={[{ sessionId: '1', sessionDate: '2026-04-01', metricType: 'WEIGHT', metricValue: 100 }]}
      />,
    );

    expect(screen.getByText('Not enough data to show trend.')).toBeInTheDocument();
  });
});

