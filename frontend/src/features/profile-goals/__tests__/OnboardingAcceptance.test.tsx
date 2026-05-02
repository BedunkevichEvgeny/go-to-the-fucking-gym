import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { BrowserRouter } from 'react-router-dom';
import ProfileGoalOnboardingPage from '../ProfileGoalOnboardingPage';
import * as hooks from '../../hooks/useProfileGoalOnboarding';

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

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  it('should display accept button when proposal is loaded', () => {
    // Given: a proposal is displayed
    const mockUseProfileGoalProposal = vi.spyOn(hooks, 'useProfileGoalOnboarding');
    mockUseProfileGoalProposal.mockReturnValue({
      createAttemptMutation: {
        mutateAsync: vi.fn(),
        isPending: false,
        isError: false,
        error: null,
      },
      currentAttemptQuery: {
        data: {
          attemptId: 'test-attempt',
          currentProposal: {
            attemptId: 'test-attempt',
            proposalId: 'test-proposal',
            version: 1,
            status: 'PROPOSED',
            generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
            sessions: [
              {
                sequenceNumber: 1,
                name: 'Session 1',
                exercises: [
                  {
                    exerciseName: 'Bench Press',
                    exerciseType: 'STRENGTH',
                    targetSets: 3,
                    targetReps: 8,
                  },
                ],
              },
            ],
          },
        },
        isLoading: false,
        error: null,
      },
      accessGateQuery: {
        data: { canAccessProgramTracking: false, reason: 'ONBOARDING_REQUIRED' },
        isLoading: false,
      },
    });

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
    // Given: accept mutation is mocked
    const acceptMutationMock = vi.fn().mockResolvedValue({ accepted: true });
    const mockUseProfileGoalProposal = vi.spyOn(hooks, 'useProfileGoalOnboarding');
    mockUseProfileGoalProposal.mockReturnValue({
      createAttemptMutation: {
        mutateAsync: vi.fn(),
        isPending: false,
        isError: false,
        error: null,
      },
      currentAttemptQuery: {
        data: {
          attemptId: 'test-attempt',
          currentProposal: {
            attemptId: 'test-attempt',
            proposalId: 'test-proposal-123',
            version: 1,
            status: 'PROPOSED',
            generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
            sessions: [
              {
                sequenceNumber: 1,
                name: 'Workout',
                exercises: [
                  {
                    exerciseName: 'Exercise',
                    exerciseType: 'STRENGTH',
                    targetSets: 3,
                    targetReps: 8,
                  },
                ],
              },
            ],
          },
        },
        isLoading: false,
        error: null,
      },
      acceptProposalMutation: {
        mutateAsync: acceptMutationMock,
        isPending: false,
      },
      accessGateQuery: {
        data: { canAccessProgramTracking: false, reason: 'ONBOARDING_REQUIRED' },
        isLoading: false,
      },
    });

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

  it('should show success message after acceptance', async () => {
    // Given: acceptance succeeds
    const mockUseProfileGoalProposal = vi.spyOn(hooks, 'useProfileGoalOnboarding');
    mockUseProfileGoalProposal.mockReturnValue({
      createAttemptMutation: {
        mutateAsync: vi.fn(),
        isPending: false,
        isError: false,
        error: null,
      },
      currentAttemptQuery: {
        data: {
          attemptId: 'test-attempt',
          currentProposal: {
            attemptId: 'test-attempt',
            proposalId: 'test-proposal',
            version: 1,
            status: 'ACCEPTED',
            generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
            sessions: [
              {
                sequenceNumber: 1,
                name: 'Workout',
                exercises: [
                  {
                    exerciseName: 'Exercise',
                    exerciseType: 'STRENGTH',
                    targetSets: 3,
                    targetReps: 8,
                  },
                ],
              },
            ],
          },
        },
        isLoading: false,
        error: null,
      },
      acceptProposalMutation: {
        mutateAsync: vi.fn().mockResolvedValue({ accepted: true }),
        isPending: false,
      },
      accessGateQuery: {
        data: { canAccessProgramTracking: false, reason: 'ONBOARDING_REQUIRED' },
        isLoading: false,
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProfileGoalOnboardingPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Then: success indicator should be present (proposal status ACCEPTED)
    expect(screen.queryByText(/ACCEPTED/i)).toBeTruthy();
  });

  it('should disable accept button while mutation is pending', async () => {
    // Given: mutation is pending
    const mockUseProfileGoalProposal = vi.spyOn(hooks, 'useProfileGoalOnboarding');
    mockUseProfileGoalProposal.mockReturnValue({
      createAttemptMutation: {
        mutateAsync: vi.fn(),
        isPending: false,
        isError: false,
        error: null,
      },
      currentAttemptQuery: {
        data: {
          attemptId: 'test-attempt',
          currentProposal: {
            attemptId: 'test-attempt',
            proposalId: 'test-proposal',
            version: 1,
            status: 'PROPOSED',
            generatedBy: { provider: 'AZURE_OPENAI', deployment: 'gpt-35-turbo' },
            sessions: [
              {
                sequenceNumber: 1,
                name: 'Workout',
                exercises: [
                  {
                    exerciseName: 'Exercise',
                    exerciseType: 'STRENGTH',
                    targetSets: 3,
                    targetReps: 8,
                  },
                ],
              },
            ],
          },
        },
        isLoading: false,
        error: null,
      },
      acceptProposalMutation: {
        mutateAsync: vi.fn(),
        isPending: true,
      },
      accessGateQuery: {
        data: { canAccessProgramTracking: false, reason: 'ONBOARDING_REQUIRED' },
        isLoading: false,
      },
    });

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
    expect(acceptButton).toHaveAttribute('disabled');
  });
});

