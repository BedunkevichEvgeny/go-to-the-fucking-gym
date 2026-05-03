import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  Link: ({ children, to }: { children: unknown; to: string }) => <a href={to}>{children as string}</a>,
}));

vi.mock('../src/components/ExerciseTable', () => ({
  ExerciseTable: () => <div />,
}));

vi.mock('../src/components/SessionTypeBadge', () => ({
  SessionTypeBadge: ({ sessionType }: { sessionType: string }) => <span>{sessionType}</span>,
}));

vi.mock('../src/features/history/ExerciseProgressionLink', () => ({
  ExerciseProgressionLink: () => <div />,
}));

const mockUseSessionDetail = vi.fn();

vi.mock('../src/hooks/useSessionDetail', () => ({
  useSessionDetail: (id: string) => mockUseSessionDetail(id),
}));

import { SessionDetailView } from '../src/features/history/SessionDetailView';

const baseSession = {
  sessionId: 'session-1',
  sessionDate: '2026-05-03',
  sessionType: 'PROGRAM' as const,
  programSessionId: 'ps-1',
  totalDurationSeconds: 3600,
  feelings: { rating: 8 },
  exerciseEntries: [],
  aiSuggestion: null,
};

describe('SessionDetailView — AI Coaching Insight', () => {
  it('PROGRAM session with non-null aiSuggestion renders suggestion section', () => {
    mockUseSessionDetail.mockReturnValue({
      data: { ...baseSession, aiSuggestion: 'Keep it up!' },
      isLoading: false,
    });

    render(<SessionDetailView sessionId="session-1" />);
    expect(screen.getByText('Keep it up!')).toBeInTheDocument();
    expect(screen.getByText('AI Coaching Insight')).toBeInTheDocument();
  });

  it('PROGRAM session with null aiSuggestion does not render suggestion section', () => {
    mockUseSessionDetail.mockReturnValue({
      data: { ...baseSession, aiSuggestion: null },
      isLoading: false,
    });

    render(<SessionDetailView sessionId="session-1" />);
    expect(screen.queryByText('AI Coaching Insight')).not.toBeInTheDocument();
  });

  it('PROGRAM session with empty string aiSuggestion does not render suggestion section', () => {
    mockUseSessionDetail.mockReturnValue({
      data: { ...baseSession, aiSuggestion: '' },
      isLoading: false,
    });

    render(<SessionDetailView sessionId="session-1" />);
    expect(screen.queryByText('AI Coaching Insight')).not.toBeInTheDocument();
  });

  it('FREE session does not render suggestion section regardless of aiSuggestion', () => {
    mockUseSessionDetail.mockReturnValue({
      data: { ...baseSession, sessionType: 'FREE' as const, aiSuggestion: 'some text' },
      isLoading: false,
    });

    render(<SessionDetailView sessionId="session-1" />);
    expect(screen.queryByText('AI Coaching Insight')).not.toBeInTheDocument();
  });

  it('FREE session with null aiSuggestion does not render suggestion section', () => {
    mockUseSessionDetail.mockReturnValue({
      data: { ...baseSession, sessionType: 'FREE' as const, aiSuggestion: null },
      isLoading: false,
    });

    render(<SessionDetailView sessionId="session-1" />);
    expect(screen.queryByText('AI Coaching Insight')).not.toBeInTheDocument();
  });
});








