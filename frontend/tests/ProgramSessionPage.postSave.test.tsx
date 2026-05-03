import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../src/hooks/useProfileGoalOnboarding', () => ({
  useTrackingAccessGate: () => ({
    isLoading: false,
    data: { canAccessProgramTracking: true },
  }),
}));

vi.mock('../src/hooks/useNextProgramSession', () => ({
  useNextProgramSession: () => ({
    data: { programSessionId: 'ps-1', sequenceNumber: 1, name: 'Push Day', exercises: [] },
    isLoading: false,
    error: null,
  }),
}));

const mockMutateAsync = vi.fn();

vi.mock('../src/hooks/useLogSession', () => ({
  useLogSession: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
  }),
}));

const mockPollHook = vi.fn();

vi.mock('../src/hooks/usePollSessionSuggestion', () => ({
  usePollSessionSuggestion: (id: string | null) => mockPollHook(id),
}));

vi.mock('../src/features/program-session/ProgramSessionForm', () => ({
  ProgramSessionForm: ({ onSubmit }: { onSubmit: (p: unknown) => Promise<void> }) => (
    <button type="button" onClick={() => onSubmit({})}>Submit</button>
  ),
}));

import { ProgramSessionPage } from '../src/pages/ProgramSessionPage';

describe('ProgramSessionPage — post-save AI suggestion states', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPollHook.mockReturnValue({ suggestion: null, timedOut: false, isPolling: false });
  });

  it('shows loading indicator while isPolling=true after save', async () => {
    mockMutateAsync.mockResolvedValue({ sessionId: 'session-123' });
    mockPollHook.mockReturnValue({ suggestion: null, timedOut: false, isPolling: true });

    render(<ProgramSessionPage />);
    fireEvent.click(screen.getByText('Submit'));

    await waitFor(() =>
      expect(screen.getByText(/Generating your coaching insight/i)).toBeInTheDocument()
    );
  });

  it('shows suggestion text when suggestion arrives', async () => {
    mockMutateAsync.mockResolvedValue({ sessionId: 'session-123' });
    mockPollHook.mockReturnValue({ suggestion: 'Well done!', timedOut: false, isPolling: false });

    render(<ProgramSessionPage />);
    fireEvent.click(screen.getByText('Submit'));

    await waitFor(() =>
      expect(screen.getByText('Well done!')).toBeInTheDocument()
    );
  });

  it('shows fallback message when timedOut=true', async () => {
    mockMutateAsync.mockResolvedValue({ sessionId: 'session-123' });
    mockPollHook.mockReturnValue({ suggestion: null, timedOut: true, isPolling: false });

    render(<ProgramSessionPage />);
    fireEvent.click(screen.getByText('Submit'));

    await waitFor(() =>
      expect(screen.getByText(/Coaching insight unavailable right now/i)).toBeInTheDocument()
    );
  });

  it('Continue to History button is always present and navigates', async () => {
    mockMutateAsync.mockResolvedValue({ sessionId: 'session-123' });
    mockPollHook.mockReturnValue({ suggestion: null, timedOut: false, isPolling: true });

    render(<ProgramSessionPage />);
    fireEvent.click(screen.getByText('Submit'));

    await waitFor(() =>
      expect(screen.getByText('Continue to History')).toBeInTheDocument()
    );

    fireEvent.click(screen.getByText('Continue to History'));
    expect(mockNavigate).toHaveBeenCalledWith('/history');
  });
});

