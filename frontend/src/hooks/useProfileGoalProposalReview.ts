import { useMutation, useQueryClient } from '@tanstack/react-query';
import { onboardingApi } from '../types/onboarding';

export function useProfileGoalProposalReview() {
  const queryClient = useQueryClient();

  const rejectMutation = useMutation({
    mutationFn: ({ proposalId, requestedChanges }: { proposalId: string; requestedChanges: string }) =>
      onboardingApi.rejectProposal(proposalId, { requestedChanges }),
    onMutate: async ({ requestedChanges }) => {
      await queryClient.cancelQueries({ queryKey: ['profile-goals', 'onboarding', 'current'] });
      const previous = queryClient.getQueryData(['profile-goals', 'onboarding', 'current']);
      return { previous, requestedChanges };
    },
    onSuccess: (nextProposal) => {
      queryClient.setQueryData(['profile-goals', 'onboarding', 'current'], (current: any) => {
        if (!current) {
          return null;
        }
        return {
          ...current,
          latestProposal: nextProposal,
        };
      });
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['profile-goals', 'onboarding', 'current'], context.previous);
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['profile-goals', 'onboarding', 'current'] });
    },
  });

  return {
    rejectProposal: rejectMutation.mutateAsync,
    isRejecting: rejectMutation.isPending,
    rejectError: rejectMutation.error,
  };
}

