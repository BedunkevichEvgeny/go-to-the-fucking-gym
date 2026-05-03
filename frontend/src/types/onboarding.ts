import { api } from '../services/api';
import type { DistanceUnit, ExerciseType, WeightUnit } from './api';

export type OnboardingPrimaryGoal = 'LOSE_WEIGHT' | 'BUILD_HEALTHY_BODY' | 'STRENGTH' | 'BUILD_MUSCLES';
export type GoalTargetBucket = 'LOSS_5' | 'LOSS_10' | 'LOSS_15' | 'LOSS_20_PLUS';
export type ProposalStatus = 'PROPOSED' | 'REJECTED' | 'ACCEPTED';
export type AttemptStatus = 'IN_PROGRESS' | 'ACCEPTED' | 'ABANDONED';

export interface OnboardingSubmissionRequest {
  age: number;
  currentWeight: number;
  weightUnit: WeightUnit;
  primaryGoal: OnboardingPrimaryGoal;
  goalTargetBucket?: GoalTargetBucket | null;
}

export interface ProposalRejectRequest {
  requestedChanges: string;
}

export interface ProposedExerciseTarget {
  exerciseName: string;
  exerciseType: ExerciseType;
  targetSets?: number | null;
  targetReps?: number | null;
  targetWeight?: number | null;
  targetWeightUnit?: WeightUnit | null;
  targetDurationSeconds?: number | null;
  targetDistance?: number | null;
  targetDistanceUnit?: DistanceUnit | null;
}

export interface ProposedSession {
  sequenceNumber: number;
  name: string;
  exercises: ProposedExerciseTarget[];
}

export interface PlanProposalResponse {
  attemptId: string;
  proposalId: string;
  version: number;
  status: ProposalStatus;
  generatedBy: {
    provider: 'AZURE_OPENAI';
    deployment: string;
  };
  sessions: ProposedSession[];
}

export interface OnboardingAttemptResponse {
  attemptId: string;
  status: AttemptStatus;
  profileGoal: OnboardingSubmissionRequest;
  latestProposal: PlanProposalResponse;
}

export interface TrackingAccessGateResponse {
  canAccessProgramTracking: boolean;
  reasonCode: 'ONBOARDING_REQUIRED' | 'ALLOWED';
  currentAttemptId?: string | null;
}

export const onboardingApi = {
  createInitialProposal: async (payload: OnboardingSubmissionRequest) => {
    const response = await api.post<PlanProposalResponse>('/profile-goals/onboarding', payload);
    return response.data;
  },
  getCurrentAttempt: async () => {
    const response = await api.get<OnboardingAttemptResponse | null>('/profile-goals/onboarding/current', {
      validateStatus: (status) => status === 200 || status === 204,
    });
    return response.status === 204 ? null : response.data;
  },
  getAccessGate: async () => {
    const response = await api.get<TrackingAccessGateResponse>('/profile-goals/access-gate');
    return response.data;
  },
  rejectProposal: async (proposalId: string, payload: ProposalRejectRequest) => {
    const response = await api.post<PlanProposalResponse>(`/profile-goals/proposals/${proposalId}/reject`, payload);
    return response.data;
  },
  acceptProposal: async (proposalId: string) => {
    const response = await api.post(`/profile-goals/proposals/${proposalId}/accept`);
    return response.data;
  },
};

