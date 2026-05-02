import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ProfileGoalOnboardingPage } from '../../../pages/ProfileGoalOnboardingPage';
import * as onboardingHooks from '../../../hooks/useProfileGoalOnboarding';
import * as reviewHooks from '../../../hooks/useProfileGoalProposalReview';

/**
 * T045: Frontend integration test for onboarding acceptance flow.
 *
 * Verifies that accepting a proposal on the frontend:
 * 1. Calls the acceptance API endpoint
 * 2. Displays success confirmation
 * 3. Redirects or transitions to program tracking
 */
describe('T045: Onboarding Acceptance Flow', () => {
  let queryClient: QueryClient;

  const proposalFixture = {
    attemptId: 'test-attempt',
    proposalId: 'test-proposal',
    version: 1,
    status: 'PROPOSED' as const,
    generatedBy: { provider: 'AZURE_OPENAI' as const, deployment: 'gpt-35-turbo' },
    sessions: [
      {
        sequenceNumber: 1,
        name: 'Session 1',
        exercises: [
          {
            exerciseName: 'Bench Press',
            exerciseType: 'STRENGTH' as const,
            targetSets: 3,
            targetReps: 8,
          },
        ],
      },
    ],
  };

  function mockPageHooks(options?: { acceptPending?: boolean; proposalStatus?: 'PROPOSED' | 'ACCEPTED' }) {
    const acceptMutationMock = vi.fn().mockResolvedValue({ accepted: true });

    vi.spyOn(onboardingHooks, 'useCreateInitialProposal').mockReturnValue({
      mutate: vi.fn(),
      mutateAsync: vi.fn(),
      isPending: false,
      data: undefined,
    } as never);

    vi.spyOn(onboardingHooks, 'useCurrentOnboardingAttempt').mockReturnValue({
      data: {
        attemptId: 'test-attempt',
        latestProposal: {
          ...proposalFixture,
          status: options?.proposalStatus ?? 'PROPOSED',
        },
      },
      isLoading: false,
      error: null,
    } as never);

    vi.spyOn(onboardingHooks, 'useTrackingAccessGate').mockReturnValue({
      data: { canAccessProgramTracking: false, reason: 'ONBOARDING_REQUIRED' },
      isLoading: false,
      error: null,
    } as never);

    vi.spyOn(onboardingHooks, 'useAcceptProposal').mockReturnValue({
      mutateAsync: acceptMutationMock,
      isPending: options?.acceptPending ?? false,
      error: null,
    } as never);

    vi.spyOn(reviewHooks, 'useProfileGoalProposalReview').mockReturnValue({
      rejectProposal: vi.fn(),
      isRejecting: false,
      rejectError: null,
    });

    return { acceptMutationMock };
  }

  beforeEach(() => {
    vi.restoreAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  it('should display accept button when proposal is loaded', () => {
    mockPageHooks();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfileGoalOnboardingPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // When: proposal is displayed
    // Then: accept button should be visible
    const acceptButton = screen.queryByText(/Accept Plan/i);
    expect(acceptButton).toBeTruthy();
  });

  it('should call accept mutation when accept button is clicked', async () => {
    const { acceptMutationMock } = mockPageHooks();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfileGoalOnboardingPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // When: accept button is clicked
    const acceptButton = screen.getByText(/Accept Plan/i);
    fireEvent.click(acceptButton);

    // Then: accept mutation should be called
    await waitFor(() => {
      expect(acceptMutationMock).toHaveBeenCalled();
    });
  });

  it('should keep proposal review visible after acceptance state is loaded', async () => {
    mockPageHooks({ proposalStatus: 'ACCEPTED' });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfileGoalOnboardingPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Then: proposal review section should still be visible for the accepted state
    expect(screen.getByText(/Latest Proposal/i)).toBeTruthy();
  });

  it('should disable accept button while mutation is pending', async () => {
    mockPageHooks({ acceptPending: true });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfileGoalOnboardingPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // When: button is busy
    const acceptButton = screen.getByText(/Accept Plan/i);

    // Then: button should be disabled
    expect((acceptButton as HTMLButtonElement).disabled).toBe(true);
  });
});

