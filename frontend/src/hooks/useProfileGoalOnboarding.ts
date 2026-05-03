import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  onboardingApi,
  type OnboardingSubmissionRequest,
  type ProposalRejectRequest,
} from '../types/onboarding';

export function useCurrentOnboardingAttempt() {
  return useQuery({
    queryKey: ['profile-goals', 'onboarding', 'current'],
    queryFn: onboardingApi.getCurrentAttempt,
  });
}

export function useTrackingAccessGate() {
  return useQuery({
    queryKey: ['profile-goals', 'access-gate'],
    queryFn: onboardingApi.getAccessGate,
  });
}

export function useCreateInitialProposal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: OnboardingSubmissionRequest) => onboardingApi.createInitialProposal(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'onboarding', 'current'] });
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'access-gate'] });
    },
  });
}

export function useRejectProposal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ proposalId, payload }: { proposalId: string; payload: ProposalRejectRequest }) =>
      onboardingApi.rejectProposal(proposalId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'onboarding', 'current'] });
    },
  });
}

export function useAcceptProposal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (proposalId: string) => onboardingApi.acceptProposal(proposalId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'onboarding', 'current'] });
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'access-gate'] });
      void queryClient.invalidateQueries({ queryKey: ['program-session', 'next'] });
    },
  });
}

